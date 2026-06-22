package com.xclone.post.controller;

import com.xclone.common.mutation.DeleteResponse;
import com.xclone.exception.GraphQlErrorMapper;
import com.xclone.exception.custom.NotPostAuthorException;
import com.xclone.exception.custom.PostNotFoundException;
import com.xclone.like.dto.LikeCount;
import com.xclone.like.service.LikeService;
import com.xclone.mention.service.MentionService;
import com.xclone.notification.model.enums.NotificationType;
import com.xclone.notification.service.NotificationService;
import com.xclone.post.dto.PostProfile;
import com.xclone.post.dto.connection.PostConnection;
import com.xclone.post.dto.mutation.PostResponse;
import com.xclone.post.dto.request.CreatePostInput;
import com.xclone.post.dto.request.UpdatePostInput;
import com.xclone.post.service.PostService;
import com.xclone.reply.dto.ReplyCount;
import com.xclone.reply.service.ReplyService;
import com.xclone.security.jwt.JwtAuthenticationFilter;
import com.xclone.security.user.CustomUserDetails;
import com.xclone.share.dto.ShareCount;
import com.xclone.share.service.ShareService;
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
  private final ShareService shareService;
  private final NotificationService notificationService;
  private final MentionService mentionService;

  public PostController(
      PostService postService,
      UserService userService,
      LikeService likeService,
      ReplyService replyService,
      ShareService shareService,
      NotificationService notificationService,
      MentionService mentionService) {
    this.postService = postService;
    this.userService = userService;
    this.likeService = likeService;
    this.replyService = replyService;
    this.shareService = shareService;
    this.notificationService = notificationService;
    this.mentionService = mentionService;
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
   * Fetches the shared post if the post entity is a post which has shared another.
   *
   * <p>The shared post is null for the original post in the post chain.
   *
   * @param posts list of posts entities
   * @return each share mapped to the original post or null
   */
  @BatchMapping(typeName = "Post", field = "sharedPost")
  public Map<PostProfile, PostProfile> sharedPost(List<PostProfile> posts) {
    List<PostProfile> sharedPosts = shareService.getSharedPosts(posts);
    Map<UUID, PostProfile> idToSharedPostMap =
        sharedPosts.stream().collect(Collectors.toMap(PostProfile::id, Function.identity()));

    Map<PostProfile, PostProfile> shareToSharedPostMap = new HashMap<>();
    posts.stream()
        .filter(share -> share.sharedPostId() != null)
        .forEach(
            share -> {
              PostProfile sharedPost = idToSharedPostMap.get(share.sharedPostId());
              if (sharedPost != null) {
                shareToSharedPostMap.put(share, sharedPost);
              }
            });
    return shareToSharedPostMap;
  }

  /**
   * Fetches the direct quotes for the queried post.
   *
   * <p>A quote is a post which has shared another post with text content.
   *
   * @param post shared post
   * @param first optional number of quotes; defaults to 10 in graphql schema
   * @param after optional cursor for cursor-pagination
   * @return a paginated list of quotes sorted descendingly by creation date
   */
  @SchemaMapping(typeName = "Post", field = "quotes")
  public PostConnection quotes(PostProfile post, @Argument Integer first, @Argument String after) {
    return shareService.getQuotes(post.id(), first, after);
  }

  /**
   * Fetches the users which reposted the queried post.
   *
   * @param post shared post
   * @param first optional number of reposts; defaults to 10 in graphql schema
   * @param after optional cursor for cursor-pagination
   * @return a paginated list of users sorted descendingly by repost creation date
   */
  @SchemaMapping(typeName = "Post", field = "reposts")
  public UserConnection reposts(PostProfile post, @Argument Integer first, @Argument String after) {
    return shareService.getRepostUsers(post.id(), first, after);
  }

  /**
   * Fetches the count of shares for each post.
   *
   * <p>Sharing a post can consist of a repost or a quote.
   *
   * @param posts list of post entities
   * @return a map of each post and its summed repost and quote count
   */
  @BatchMapping(typeName = "Post", field = "shareCount")
  public Map<PostProfile, Integer> shareCount(List<PostProfile> posts) {
    List<UUID> postIds = posts.stream().map(PostProfile::id).toList();
    List<ShareCount> shareCounts = shareService.getShareCounts(postIds);
    Map<UUID, Integer> shareCountPerPost =
        shareCounts.stream()
            .collect(Collectors.toMap(ShareCount::sharedPostId, ShareCount::numberOfShares));

    return posts.stream()
        .collect(
            Collectors.toMap(
                Function.identity(), post -> shareCountPerPost.getOrDefault(post.id(), 0)));
  }

  /**
   * Fetches if the authenticated user has shared each queried post.
   *
   * <p>Sharing a post can consist of a repost or a quote.
   *
   * @param posts list of post entities
   * @return each post with {@code sharedByMe=true} if the user has reposted or quoted the post
   */
  @BatchMapping(typeName = "Post", field = "sharedByMe")
  public Map<PostProfile, Boolean> sharedByMe(List<PostProfile> posts) {
    CustomUserDetails userDetails =
        (CustomUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    List<UUID> postIds = posts.stream().map(PostProfile::id).toList();
    Set<UUID> postIdsThatUserShared =
        shareService.getSharedIdsInPosts(postIds, userDetails.getId());

    return posts.stream()
        .collect(
            Collectors.toMap(
                Function.identity(), post -> postIdsThatUserShared.contains(post.id())));
  }

  /**
   * Fetches the users who are mentioned as part of the {@link PostProfile#messageContent()}.
   *
   * @param posts list of post entities
   * @return each post mapped to the users mentioned in its message content
   */
  @BatchMapping(typeName = "Post", field = "mentions")
  public Map<PostProfile, List<UserProfile>> mentions(List<PostProfile> posts) {
    List<UUID> postIds = posts.stream().map(PostProfile::id).toList();
    Map<UUID, List<UserProfile>> postMentions = mentionService.getPostMentions(postIds);
    return posts.stream()
        .collect(
            Collectors.toMap(
                Function.identity(), post -> postMentions.getOrDefault(post.id(), List.of())));
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
      PostProfile deletedPost = postService.deletePost(postId, userDetails.getId());
      PostType postType = discernPostType(deletedPost);
      if (postType != PostType.POST) {
        PostProfile originalPost = getOriginalPost(deletedPost, postType);
        if (originalPost != null) {
          notificationService.deleteNotificationActorAndCleanupNotification(
              userDetails.getId(),
              originalPost.authorId(),
              postType.toNotificationType(),
              originalPost.id());
        }
      } else {
        // delete all notifications related to post on post-deletion
        notificationService.deletePostNotifications(deletedPost.id());
      }
      return new DeleteResponse("200", true, null);
    } catch (NotPostAuthorException ex) {
      return new DeleteResponse(
          "403", false, GraphQlErrorMapper.fromNotPostAuthor("deletePost", ex));
    } catch (PostNotFoundException ex) {
      return new DeleteResponse(
          "404", false, GraphQlErrorMapper.fromPostNotFound("deletePost", ex));
    }
  }

  private PostType discernPostType(PostProfile post) {
    if (post.parentId() != null) {
      return PostType.REPLY;
    }
    if (post.sharedPostId() != null && post.messageContent() != null) {
      return PostType.QUOTE;
    }
    if (post.sharedPostId() != null) {
      return PostType.REPOST;
    }
    return PostType.POST;
  }

  private PostProfile getOriginalPost(PostProfile post, PostType type) {
    UUID postId;
    if (type == PostType.REPLY) {
      postId = post.parentId();
    } else {
      // Must be a shared post type - QUOTE / REPOST
      postId = post.sharedPostId();
    }
    return getPost(postId);
  }

  /** Enum for each type of post. */
  private enum PostType {
    REPOST,
    QUOTE,
    REPLY,
    POST;

    /**
     * Mapping method to convert the {@link PostType} to its corresponding {@link NotificationType}.
     *
     * <p>{@link PostType} contains a subset of {@link NotificationType} values.
     *
     * @return corresponding notification type
     */
    private NotificationType toNotificationType() {
      return switch (this) {
        case REPLY -> NotificationType.REPLY;
        case REPOST -> NotificationType.REPOST;
        case QUOTE -> NotificationType.QUOTE;
        case POST -> throw new IllegalStateException("POST has no corresponding notification type");
      };
    }
  }
}
