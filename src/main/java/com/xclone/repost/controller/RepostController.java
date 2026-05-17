package com.xclone.repost.controller;

import com.xclone.exception.GraphQlErrorMapper;
import com.xclone.exception.custom.PostNotFoundException;
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

/** GraphQL controller for repost-related operations. */
@Controller
public class RepostController {
  private final PostService postService;

  RepostController(PostService postService) {
    this.postService = postService;
  }

  /**
   * Triggers {@link PostService#createRepost(UUID, UUID)} with the authenticated user as the author
   * of the post.
   *
   * <p>Method used to create a simple repost i.e. a post with no message content.
   *
   * @param userDetails authenticated user; populated as part of the security chain with {@link
   *     JwtAuthenticationFilter}
   * @param postId unique identifier of the reposted post
   * @return the created post
   */
  @MutationMapping
  public PostResponse createRepost(
      @AuthenticationPrincipal CustomUserDetails userDetails, @Argument UUID postId) {
    try {
      PostProfile repost = postService.createRepost(postId, userDetails.getId());
      return new PostResponse("201", true, repost, null);
    } catch (PostNotFoundException ex) {
      return new PostResponse(
          "404", false, null, GraphQlErrorMapper.fromPostNotFound("postId", ex));
    }
  }
}
