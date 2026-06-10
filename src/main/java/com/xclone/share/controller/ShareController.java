package com.xclone.share.controller;

import com.xclone.exception.GraphQlErrorMapper;
import com.xclone.exception.custom.DuplicateRepostException;
import com.xclone.exception.custom.PostNotFoundException;
import com.xclone.notification.model.enums.NotificationType;
import com.xclone.notification.service.NotificationService;
import com.xclone.post.dto.PostProfile;
import com.xclone.post.dto.mutation.PostResponse;
import com.xclone.post.service.PostService;
import com.xclone.security.jwt.JwtAuthenticationFilter;
import com.xclone.security.user.CustomUserDetails;
import com.xclone.share.dto.request.CreateQuoteInput;
import jakarta.validation.ConstraintViolationException;
import java.util.UUID;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;

/** GraphQL controller for share-related operations. */
@Controller
public class ShareController {
  private final PostService postService;
  private final NotificationService notificationService;

  ShareController(PostService postService, NotificationService notificationService) {
    this.postService = postService;
    this.notificationService = notificationService;
  }

  /**
   * Triggers {@link PostService#createRepost(UUID, UUID)} with the authenticated user as the author
   * of the post.
   *
   * <p>Method used to create a simple repost i.e. a post with no message content.
   *
   * @param userDetails authenticated user; populated as part of the security chain with {@link
   *     JwtAuthenticationFilter}
   * @param sharedPostId unique identifier of the reposted post
   * @return the created post
   */
  @MutationMapping
  public PostResponse createRepost(
      @AuthenticationPrincipal CustomUserDetails userDetails, @Argument UUID sharedPostId) {
    try {
      PostProfile repost = postService.createRepost(sharedPostId, userDetails.getId());
      PostProfile sharedPost = postService.getPost(sharedPostId);
      notificationService.upsertNotification(
          sharedPost.authorId(), userDetails.getId(), repost.id(), NotificationType.REPOST);
      return new PostResponse("200", true, repost, null);
    } catch (DuplicateRepostException ex) {
      return new PostResponse("400", false, null, GraphQlErrorMapper.fromDuplicateRepost(ex));
    } catch (PostNotFoundException ex) {
      return new PostResponse(
          "404", false, null, GraphQlErrorMapper.fromPostNotFound("sharedPostId", ex));
    }
  }

  /**
   * Triggers {@link PostService#createQuote(CreateQuoteInput, UUID)} with the authenticated user as
   * the author of the post.
   *
   * <p>Method used to create a quote i.e. a repost with message content.
   *
   * @param userDetails authenticated user; populated as part of the security chain with {@link
   *     JwtAuthenticationFilter}
   * @param input DTO containing the information of the quote
   * @return the created post
   */
  @MutationMapping
  public PostResponse createQuote(
      @AuthenticationPrincipal CustomUserDetails userDetails, @Argument CreateQuoteInput input) {
    try {
      PostProfile quote = postService.createQuote(input, userDetails.getId());
      PostProfile sharedPost = postService.getPost(input.sharedPostId());
      notificationService.upsertNotification(
          sharedPost.authorId(), userDetails.getId(), quote.id(), NotificationType.QUOTE);
      return new PostResponse("200", true, quote, null);
    } catch (ConstraintViolationException ex) {
      return new PostResponse("400", false, null, GraphQlErrorMapper.fromConstraintViolations(ex));
    } catch (PostNotFoundException ex) {
      return new PostResponse(
          "404", false, null, GraphQlErrorMapper.fromPostNotFound("sharedPostId", ex));
    }
  }
}
