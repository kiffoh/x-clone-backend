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
import java.util.function.Function;
import java.util.stream.Collectors;
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

  /**
   * Obtains the user entities for the author of each post.
   *
   * @param posts list of posts
   * @return each post mapped to the authors user profile
   */
  @BatchMapping(typeName = "Post", field = "author")
  public Map<PostProfile, UserProfile> author(List<PostProfile> posts) {
    List<UUID> authorIds = posts.stream().map(PostProfile::authorId).toList();
    List<UserProfile> users = userService.getUsersById(authorIds);
    Map<UUID, UserProfile> uuidUserMap =
        users.stream().collect(Collectors.toMap(UserProfile::id, Function.identity()));

    return posts.stream()
        .collect(Collectors.toMap(Function.identity(), post -> uuidUserMap.get(post.authorId())));
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
