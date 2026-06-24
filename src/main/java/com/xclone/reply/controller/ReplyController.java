package com.xclone.reply.controller;

import com.xclone.exception.GraphQlErrorMapper;
import com.xclone.exception.custom.PostNotFoundException;
import com.xclone.mention.service.MentionService;
import com.xclone.notification.model.enums.NotificationType;
import com.xclone.notification.service.NotificationService;
import com.xclone.post.dto.PostProfile;
import com.xclone.post.dto.mutation.PostResponse;
import com.xclone.post.service.PostService;
import com.xclone.reply.dto.ReplyThread;
import com.xclone.reply.dto.request.CreateReplyInput;
import com.xclone.reply.service.ReplyService;
import com.xclone.security.jwt.JwtAuthenticationFilter;
import com.xclone.security.user.CustomUserDetails;
import jakarta.validation.ConstraintViolationException;
import java.util.List;
import java.util.UUID;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;

/** GraphQL controller for reply-related operations. */
@Controller
public class ReplyController {
  private final ReplyService replyService;
  private final PostService postService;
  private final NotificationService notificationService;
  private final MentionService mentionService;

  ReplyController(
      ReplyService replyService,
      PostService postService,
      NotificationService notificationService,
      MentionService mentionService) {
    this.replyService = replyService;
    this.postService = postService;
    this.notificationService = notificationService;
    this.mentionService = mentionService;
  }

  /**
   * Fetches the specific reply thread for the queried {@code postId}.
   *
   * @param postId unique identifier of the starting post in a reply thread
   * @return a 2D list of posts, one of ancestors and one of siblings, both sorted by creation date
   *     ascendingly
   */
  @QueryMapping
  public ReplyThread getReplyThread(@Argument UUID postId) {
    PostProfile post = postService.getPost(postId);
    if (post == null) {
      return null;
    }
    if (post.parentId() == null) {
      return new ReplyThread(List.of(), List.of(), post);
    }

    return replyService.getReplyThread(post);
  }

  /**
   * Triggers {@link PostService#createReply(CreateReplyInput, UUID)} with the authenticated user as
   * the author of the post.
   *
   * <p>If {@code mentionedUserIds} is provided, creates mention rows for active users and triggers
   * a {@link NotificationType#MENTION} notification for each. Also triggers a {@link
   * NotificationType#REPLY} notification for the parent post's author.
   *
   * @param userDetails authenticated user; populated as part of the security chain with {@link
   *     JwtAuthenticationFilter}
   * @param input DTO containing the content and parent id of the post
   * @return the created post
   */
  @MutationMapping
  public PostResponse createReply(
      @AuthenticationPrincipal CustomUserDetails userDetails, @Argument CreateReplyInput input) {
    try {
      PostProfile reply = postService.createReply(input, userDetails.getId());
      List<UUID> mentionedUserIds = input.mentionedUserIds();
      if (mentionedUserIds != null && !mentionedUserIds.isEmpty()) {
        List<UUID> createdMentionUserIds =
            mentionService.createMentions(reply.id(), mentionedUserIds);
        createdMentionUserIds.forEach(
            mentionedUserId ->
                notificationService.upsertNotification(
                    mentionedUserId, userDetails.getId(), reply.id(), NotificationType.MENTION));
      }
      PostProfile parentPost = postService.getPost(input.parentId());
      notificationService.upsertNotification(
          parentPost.authorId(), userDetails.getId(), parentPost.id(), NotificationType.REPLY);
      return new PostResponse("200", true, reply, null);
    } catch (ConstraintViolationException ex) {
      return new PostResponse("400", false, null, GraphQlErrorMapper.fromConstraintViolations(ex));
    } catch (PostNotFoundException ex) {
      return new PostResponse(
          "404", false, null, GraphQlErrorMapper.fromPostNotFound("parentId", ex));
    }
  }
}
