package com.xclone.post.controller;

import com.xclone.common.mutation.DeleteResponse;
import com.xclone.exception.GraphQlErrorMapper;
import com.xclone.exception.custom.NotPostAuthorException;
import com.xclone.exception.custom.PostNotFoundException;
import com.xclone.like.dto.LikeCount;
import com.xclone.like.service.LikeService;
import com.xclone.post.dto.PostProfile;
import com.xclone.post.dto.connection.PostConnection;
import com.xclone.post.dto.mutation.PostResponse;
import com.xclone.post.dto.request.CreatePostInput;
import com.xclone.post.dto.request.UpdatePostInput;
import com.xclone.post.service.PostService;
import com.xclone.reply.dto.ReplyCount;
import com.xclone.reply.service.ReplyService;
import com.xclone.repost.service.RepostService;
import com.xclone.security.jwt.JwtAuthenticationFilter;
import com.xclone.security.user.CustomUserDetails;
import com.xclone.user.dto.UserProfile;
import com.xclone.user.dto.connection.UserConnection;
import com.xclone.user.service.UserService;
import jakarta.validation.ConstraintViolationException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.BatchMapping;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;

/** GraphQL controller for post-related operations. */
@Controller
public class PostController {
  private final PostService postService;
  private final UserService userService;
  private final LikeService likeService;
  private final ReplyService replyService;
  private final RepostService repostService;

  public PostController(
      PostService postService,
      UserService userService,
      LikeService likeService,
      ReplyService replyService,
      RepostService repostService) {
    this.postService = postService;
    this.userService = userService;
    this.likeService = likeService;
    this.replyService = replyService;
    this.repostService = repostService;
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
   * Fetches the number of likes for each queried post.
   *
   * @param posts list of {@link PostProfile} entities
   * @return each post with the amount of likes it has
   */
  @BatchMapping(typeName = "Post", field = "likeCount")
  public Map<PostProfile, Integer> likeCount(List<PostProfile> posts) {
    List<UUID> postIds = posts.stream().map(PostProfile::id).toList();
    List<LikeCount> likeCounts = likeService.getLikeCounts(postIds);
    Map<UUID, Integer> likeCountPerPost =
        likeCounts.stream().collect(Collectors.toMap(LikeCount::postId, LikeCount::numberOfLikes));

    return posts.stream()
        .collect(
            Collectors.toMap(
                Function.identity(), post -> likeCountPerPost.getOrDefault(post.id(), 0)));
  }

  /**
   * Fetches if the authenticated user has liked each queried post.
   *
   * @param posts list of {@link PostProfile} entities
   * @return each post with {@code likedByMe=true} if the user has liked the post
   */
  @BatchMapping(typeName = "Post", field = "likedByMe")
  public Map<PostProfile, Boolean> likedByMe(List<PostProfile> posts) {
    CustomUserDetails userDetails =
        (CustomUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    List<UUID> postIds = posts.stream().map(PostProfile::id).toList();
    Set<UUID> postIdsThatUserLikes =
        likeService.getPostIdsThatUserLikes(postIds, userDetails.getId());

    return posts.stream()
        .collect(
            Collectors.toMap(
                Function.identity(), post -> postIdsThatUserLikes.contains(post.id())));
  }

  /**
   * Fetches the users which have liked a post.
   *
   * @param post {@link PostProfile} representing the queried post
   * @return a paginated list of users sorted by like creation date descending
   */
  @SchemaMapping(typeName = "Post", field = "likes")
  public UserConnection likes(PostProfile post, @Argument Integer first, @Argument String after) {
    CustomUserDetails userDetails =
        (CustomUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    return likeService.getUsersThatLikedPost(userDetails.getId(), post, first, after);
  }

  /**
   * Fetches the parent post for the queried post.
   *
   * <p>The parent is null if the post is the original post in the post chain.
   *
   * @param post {@link PostProfile} entity
   * @return the parent post or null
   */
  @SchemaMapping(typeName = "Post", field = "parent")
  public PostProfile parent(PostProfile post) {
    if (post.parentId() == null) {
      return null;
    }
    return postService.getPost(post.parentId());
  }

  /**
   * Retrieves the direct reply count for each post.
   *
   * @param posts list of posts
   * @return a map of each post and its respective reply count
   */
  @BatchMapping(typeName = "Post", field = "replyCount")
  public Map<PostProfile, Integer> replyCount(List<PostProfile> posts) {
    List<UUID> postIds = posts.stream().map(PostProfile::id).toList();
    List<ReplyCount> replyCounts = replyService.getReplyCounts(postIds);
    Map<UUID, Integer> replyCountPerPost =
        replyCounts.stream()
            .collect(Collectors.toMap(ReplyCount::postId, ReplyCount::numberOfReplies));

    return posts.stream()
        .collect(
            Collectors.toMap(
                Function.identity(), post -> replyCountPerPost.getOrDefault(post.id(), 0)));
  }

  /**
   * Fetches the direct replies for the queried post.
   *
   * @param post parent post
   * @param first optional number of posts; defaults to 10 in graphql schema
   * @param after optional cursor for cursor-pagination
   * @return a paginated list of posts sorted descendingly by creation date TODO: update sort order
   *     to be most interacted with first
   */
  @SchemaMapping(typeName = "Post", field = "replies")
  public PostConnection replies(PostProfile post, @Argument Integer first, @Argument String after) {
    return replyService.getReplies(post.id(), first, after);
  }

  /**
   * Fetches the quoted post for the quote entity.
   *
   * <p>The quoted post is null for the original post in the post chain.
   *
   * @param quotes list of quote posts
   * @return each quote mapped to the original post or null
   */
  @BatchMapping(typeName = "Post", field = "quotedPost")
  public Map<PostProfile, PostProfile> quotedPost(List<PostProfile> quotes) {
    List<PostProfile> quotedPosts = repostService.getQuotedPosts(quotes);
    Map<UUID, PostProfile> idToQuotedPostMap =
        quotedPosts.stream().collect(Collectors.toMap(PostProfile::id, Function.identity()));
    Map<PostProfile, PostProfile> quoteToQuotedPostMap = new HashMap<>();

    quotes.forEach(
        quote -> {
          PostProfile quotedPost = idToQuotedPostMap.getOrDefault(quote.quotedPostId(), null);
          quoteToQuotedPostMap.put(quote, quotedPost);
        });
    return quoteToQuotedPostMap;
  }

  /**
   * Fetches the direct quotes for the queried post.
   *
   * @param post parent post
   * @param first optional number of quotes; defaults to 10 in graphql schema
   * @param after optional cursor for cursor-pagination
   * @return a paginated list of quotes sorted descendingly by creation date
   */
  @SchemaMapping(typeName = "Post", field = "quotes")
  public PostConnection quotes(PostProfile post, @Argument Integer first, @Argument String after) {
    return repostService.getQuotes(post.id(), first, after);
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
    try {
      PostProfile post = postService.createPost(input, userDetails.getId());
      return new PostResponse("200", true, post, null);
    } catch (ConstraintViolationException ex) {
      return new PostResponse("400", false, null, GraphQlErrorMapper.fromConstraintViolations(ex));
    }
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
    } catch (NotPostAuthorException ex) {
      return new PostResponse(
          "403", false, null, GraphQlErrorMapper.fromNotPostAuthor("updatePostContent", ex));
    } catch (PostNotFoundException ex) {
      return new PostResponse(
          "404", false, null, GraphQlErrorMapper.fromPostNotFound("updatePostContent", ex));
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
    } catch (NotPostAuthorException ex) {
      return new DeleteResponse(
          "403", false, GraphQlErrorMapper.fromNotPostAuthor("deletePost", ex));
    } catch (PostNotFoundException ex) {
      return new DeleteResponse(
          "404", false, GraphQlErrorMapper.fromPostNotFound("deletePost", ex));
    }
  }
}
