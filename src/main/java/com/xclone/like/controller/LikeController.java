package com.xclone.like.controller;

import com.xclone.exception.GraphQlErrorMapper;
import com.xclone.exception.custom.PostNotFoundException;
import com.xclone.like.service.LikeService;
import com.xclone.post.dto.PostProfile;
import com.xclone.post.dto.mutation.PostResponse;
import com.xclone.post.service.PostService;
import com.xclone.security.jwt.JwtAuthenticationFilter;
import com.xclone.security.user.CustomUserDetails;
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

  public LikeController(LikeService likeService, PostService postService) {
    this.likeService = likeService;
    this.postService = postService;
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
      return new PostResponse("201", true, updatedPost, null);
    } catch (PostNotFoundException ex) {
      return new PostResponse(
          "400", false, null, GraphQlErrorMapper.fromPostNotFound("postId", ex));
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
      return new PostResponse("200", true, updatedPost, null);
    } else {
      return new PostResponse(
          "404",
          false,
          null,
          GraphQlErrorMapper.fromPostNotFound(
              "postId", new PostNotFoundException("Post does not exist for queried postId")));
    }
  }
}
