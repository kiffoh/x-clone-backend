package com.xclone.post.controller;

import com.xclone.common.mutation.DeleteResponse;
import com.xclone.exception.GraphQlErrorMapper;
import com.xclone.post.dto.PostProfile;
import com.xclone.post.dto.connection.PostConnection;
import com.xclone.post.dto.mutation.PostResponse;
import com.xclone.post.dto.request.CreatePostInput;
import com.xclone.post.dto.request.UpdatePostInput;
import com.xclone.post.service.PostService;
import com.xclone.security.jwt.JwtAuthenticationFilter;
import com.xclone.security.user.CustomUserDetails;
import com.xclone.user.dto.UserProfile;
import com.xclone.user.service.UserService;
import jakarta.validation.ConstraintViolationException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.BatchMapping;
import org.springframework.graphql.data.method.annotation.MutationMapping;
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
    List<UserProfile> users = userService.getActiveUsersById(authorIds);
    Map<UUID, UserProfile> uuidUserMap =
        users.stream().collect(Collectors.toMap(UserProfile::id, Function.identity()));

    Map<PostProfile, UserProfile> authors = new HashMap<>();
    posts.forEach(
        post -> {
          UserProfile author = uuidUserMap.get(post.authorId());
          if (author != null) {
            authors.put(post, author);
          }
        });
    return authors;
  }

  /**
   * Resolves the graphql getPost query.
   *
   * @param postId unique identifier of the post
   * @return public facing {@link PostProfile} dto
   */
  @QueryMapping
  public PostProfile getPost(@Argument UUID postId) {
    return postService.getPost(postId);
  }

  /**
   * Resolves the graphql get feed query.
   *
   * @param userDetails authenticated user set in the security context
   * @param first optional number of posts; defaults to 10 in graphql schema
   * @param after optional cursor for cursor-pagination
   * @return a paginated list of posts
   */
  @QueryMapping
  public PostConnection feed(
      @AuthenticationPrincipal CustomUserDetails userDetails,
      @Argument Integer first,
      @Argument String after) {
    return postService.getFeed(userDetails.getId(), first, after);
  }

  /**
   * Triggers {@link PostService#createPost(CreatePostInput, UUID)} with the authenticated user as
   * the author of the post.
   *
   * @param userDetails authenticated user; populated as part of the security chain with {@link
   *     JwtAuthenticationFilter}
   * @param input DTO containing the content of the post
   * @return the created post
   */
  @MutationMapping
  public PostResponse createPost(
      @AuthenticationPrincipal CustomUserDetails userDetails, @Argument CreatePostInput input) {
    PostProfile post = postService.createPost(input, userDetails.getId());
    return new PostResponse("200", true, post, null);
  }

  /**
   * Triggers {@link PostService#updatePost(UpdatePostInput, UUID)} with the authenticated user as
   * the author of the post.
   *
   * <p>Business exceptions are mapped with {@link GraphQlErrorMapper} in the style of
   * "errors-as-data".
   *
   * @param userDetails authenticated user; populated as part of the security chain with {@link
   *     JwtAuthenticationFilter}
   * @param input DTO containing the post details to update
   * @return the updated post
   */
  @MutationMapping
  public PostResponse updatePostContent(
      @AuthenticationPrincipal CustomUserDetails userDetails, @Argument UpdatePostInput input) {
    try {
      PostProfile updatedPost = postService.updatePost(input, userDetails.getId());
      return new PostResponse("200", true, updatedPost, null);
    } catch (IllegalAccessException ex) {
      return new PostResponse(
          "403", false, null, GraphQlErrorMapper.fromIllegalAccess("updatePost", ex));
    } catch (ConstraintViolationException ex) {
      return new PostResponse("400", false, null, GraphQlErrorMapper.fromConstraintViolations(ex));
    }
  }

  /**
   * Triggers {@link PostService#deletePost(UUID, UUID)}} with the post id and the authenticated
   * user as the author of the post.
   *
   * <p>Business exceptions are mapped with {@link GraphQlErrorMapper} in the style of
   * "errors-as-data".
   *
   * @param userDetails authenticated user; populated as part of the security chain with {@link
   *     JwtAuthenticationFilter}
   * @param postId unique identifier of the post to be deleted
   * @return a successful delete response
   */
  @MutationMapping
  public DeleteResponse deletePost(
      @AuthenticationPrincipal CustomUserDetails userDetails, @Argument UUID postId) {
    try {
      postService.deletePost(postId, userDetails.getId());
      return new DeleteResponse("200", true, null);
    } catch (IllegalAccessException ex) {
      return new DeleteResponse(
          "403", false, GraphQlErrorMapper.fromIllegalAccess("deletePost", ex));
    }
  }
}
