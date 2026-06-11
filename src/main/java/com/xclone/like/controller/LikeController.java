package com.xclone.like.controller;

import com.xclone.exception.GraphQlErrorMapper;
import com.xclone.exception.custom.PostNotFoundException;
import com.xclone.exception.dto.FieldError;
import com.xclone.like.service.LikeService;
import com.xclone.notification.model.enums.NotificationType;
import com.xclone.notification.service.NotificationService;
import com.xclone.post.dto.PostProfile;
import com.xclone.post.dto.mutation.PostResponse;
import com.xclone.post.service.PostService;
import com.xclone.security.jwt.JwtAuthenticationFilter;
import com.xclone.security.user.CustomUserDetails;
import java.util.List;
import java.util.UUID;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;

/** GraphQL controller for like entity operations. */
@Controller
public class LikeController {
  private final LikeService likeService;
  private final PostService postService;
  private final NotificationService notificationService;

  public LikeController(
      LikeService likeService, PostService postService, NotificationService notificationService) {
    this.likeService = likeService;
    this.postService = postService;
    this.notificationService = notificationService;
  }

  /**
   * Adds a like to a post on behalf of the authenticated user.
   *
   * @param userDetails authenticated user; populated as part of the security chain with {@link
   *     JwtAuthenticationFilter}
   * @param postId unique identifier of the post to add a like to
   * @return updated post with the additional like
   */
  @MutationMapping
  public PostResponse likePost(
      @AuthenticationPrincipal CustomUserDetails userDetails, @Argument UUID postId) {
    try {
      likeService.createLike(postId, userDetails.getId());
      PostProfile updatedPost = postService.getPost(postId);
      // Don't notify on self like
      if (!updatedPost.authorId().equals(userDetails.getId())) {
        notificationService.upsertNotification(
            updatedPost.authorId(), userDetails.getId(), updatedPost.id(), NotificationType.LIKE);
      }
      return new PostResponse("201", true, updatedPost, null);
    } catch (PostNotFoundException ex) {
      return new PostResponse(
          "404", false, null, GraphQlErrorMapper.fromPostNotFound("postId", ex));
    }
  }

  /**
   * Removes a like from a post on behalf of the authenticated user.
   *
   * <p>Returns a null post if the post cannot be found.
   *
   * @param userDetails authenticated user; populated as part of the security chain with {@link
   *     JwtAuthenticationFilter}
   * @param postId unique identifier of the post to remove a like from
   * @return updated post with the like removed
   */
  @MutationMapping
  public PostResponse unlikePost(
      @AuthenticationPrincipal CustomUserDetails userDetails, @Argument UUID postId) {
    likeService.deleteLike(postId, userDetails.getId());
    PostProfile updatedPost = postService.getPost(postId);
    if (updatedPost != null) {
      notificationService.deleteNotificationActorAndCleanupNotification(
          userDetails.getId(), updatedPost.authorId(), NotificationType.LIKE, updatedPost.id());
      return new PostResponse("200", true, updatedPost, null);
    } else {
      FieldError postNotFound = new FieldError("postId", "Post does not exist");
      return new PostResponse("404", false, null, List.of(postNotFound));
    }
  }
}
