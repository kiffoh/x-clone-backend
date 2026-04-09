package com.xclone.post.controller;

import com.xclone.post.dto.PostProfile;
import com.xclone.post.dto.connection.PostConnection;
import com.xclone.post.service.PostService;
import com.xclone.security.user.CustomUserDetails;
import com.xclone.user.dto.UserProfile;
import com.xclone.user.service.UserService;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.BatchMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;

/** GraphQL controller for post-related operations. */
@Controller
public class PostController {
  private final PostService postService;
  private final UserService userService;

  public PostController(PostService postService, UserService userService) {
    this.postService = postService;
    this.userService = userService;
  }

  @BatchMapping(typeName = "Post", field = "author")
  public Map<PostProfile, UserProfile> author(List<PostProfile> posts) {
    return userService.getAuthorsFromPosts(posts);
  }

  @QueryMapping
  public PostProfile getPost(@Argument UUID postId) {
    return postService.getPost(postId);
  }

  @QueryMapping
  public PostConnection feed(
      @AuthenticationPrincipal CustomUserDetails userDetails,
      @Argument Integer first,
      @Argument String after) {
    return postService.getFeed(userDetails.getId(), first, after);
  }
}
