package com.xclone.post.service;

import com.xclone.common.connection.Cursor;
import com.xclone.common.connection.PageInfo;
import com.xclone.common.enums.Status;
import com.xclone.exception.custom.DuplicateRepostException;
import com.xclone.exception.custom.NotPostAuthorException;
import com.xclone.exception.custom.PostNotFoundException;
import com.xclone.follow.service.FollowService;
import com.xclone.post.dto.PostProfile;
import com.xclone.post.dto.connection.PostConnection;
import com.xclone.post.dto.connection.PostEdge;
import com.xclone.post.dto.request.CreatePostInput;
import com.xclone.post.dto.request.UpdatePostInput;
import com.xclone.post.model.entity.Post;
import com.xclone.post.repository.PostRepository;
import com.xclone.reply.dto.request.CreateReplyInput;
import com.xclone.share.dto.request.CreateQuoteInput;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

/** Service layer responsible for post-related operations. */
@Service
@Validated
public class PostService {
  private final PostRepository postRepository;
  private final FollowService followService;

  public PostService(PostRepository postRepository, FollowService followService) {
    this.postRepository = postRepository;
    this.followService = followService;
  }

  /**
   * Adds paginated metadata to each post and separate metadata about the context of the post.
   *
   * @param posts a list of posts
   * @return a paginated list of posts
   */
  public static PostConnection toPostConnection(Slice<Post> posts) {
    List<PostEdge> edges =
        posts.stream()
            .map(
                post -> {
                  Cursor cursor = new Cursor(post.getCreatedAt(), post.getId());
                  return new PostEdge(post.toPostProfile(), cursor.encode());
                })
            .toList();
    PageInfo pageInfo =
        new PageInfo(
            posts.hasNext(),
            posts.hasPrevious(),
            posts.isEmpty() ? null : edges.getFirst().cursor(),
            posts.isEmpty() ? null : edges.getLast().cursor());
    return new PostConnection(edges, pageInfo);
  }

  /**
   * Maps a post to a {@link PostProfile} record if the post has {@code status == Status.ACTIVE} or
   * returns null if inactive.
   *
   * @param post post entity to be evaluated if active
   * @return the {@link PostProfile} record or null
   */
  public static PostProfile mapIfActive(Post post) {
    return post.getStatus() == Status.ACTIVE ? post.toPostProfile() : null;
  }

  /**
   * Fetches the {@link PostProfile} for a valid and active post.
   *
   * <p>Returns null if the post is not active or when the post does not exist.
   *
   * @param id unique identifier of the post
   * @return a post
   */
  public PostProfile getPost(UUID id) {
    Optional<Post> post = postRepository.findActivePostById(id);
    return post.map(Post::toPostProfile).orElse(null);
  }

  /**
   * Fetches the {@link PostProfile} for a valid and active posts.
   *
   * <p>Returns null if a post is not active/does not exist.
   *
   * @param postIds unique identifier of the posts
   * @return a list of {@link PostProfile} entities
   */
  public List<PostProfile> getActivePostsFromIds(List<UUID> postIds) {
    List<Post> posts = postRepository.findActivePostsById(postIds);
    return posts.stream().map(Post::toPostProfile).toList();
  }

  // First implementation is just posts from followed users
  // Later iterations will use more information to get better posts

  /**
   * Fetches a paginated list of posts from accounts that the authenticated user follows. TODO:
   * update Javadoc when iterated
   *
   * <p>The current result excludes:
   *
   * <ul>
   *   <li>Posts from users not followed by the authenticated user
   *   <li>Posts from the authenticated user
   * </ul>
   *
   * <p>Note: no transaction boundary is required as no lazy associations are traversed.
   *
   * @param userId unique UUID for user entity
   * @param first desired number of results
   * @param after optional cursor of where the previous pagination finished
   * @return a list of posts sorted by creation date
   */
  public PostConnection getFeed(UUID userId, Integer first, String after) {
    Pageable pageable = Pageable.ofSize(first);
    List<UUID> followingIds = followService.getFollowingIds(userId);
    Slice<Post> feed;
    if (after == null) {
      feed = postRepository.findFirstPageOfFeed(userId, followingIds, pageable);
    } else {
      Cursor cursor = Cursor.toCursor(after);
      feed =
          postRepository.findNextPageOfFeed(
              userId, followingIds, cursor.id(), cursor.timestamp(), pageable);
    }
    return toPostConnection(feed);
  }

  /**
   * Creates a post entity with the provided input fields using a {@link Transactional} view,
   * ensuring for atomicity and that dirty checking applies.
   *
   * @param input DTO with post details to be created
   * @param authorId unique uuid of the authenticated user
   * @return the created post
   */
  @Transactional
  public PostProfile createPost(@Valid CreatePostInput input, UUID authorId) {
    Post post = new Post();
    post.setAuthorId(authorId);
    post.setMessageContent(input.messageContent());
    Post savedPost = postRepository.save(post);
    return savedPost.toPostProfile();
  }

  /**
   * Creates a reply entity with the provided input fields using a {@link Transactional} view,
   * ensuring for atomicity and that dirty checking applies.
   *
   * @param input DTO with post details to be created
   * @param authorId unique uuid of the authenticated user
   * @return the created reply
   */
  @Transactional
  public PostProfile createReply(@Valid CreateReplyInput input, UUID authorId) {
    Optional<Post> parent = postRepository.findById(input.parentId());
    if (parent.isEmpty()) {
      throw new PostNotFoundException("Parent post cannot be found");
    }

    Post reply = new Post();
    reply.setAuthorId(authorId);
    reply.setMessageContent(input.messageContent());
    reply.setParentId(input.parentId());
    Post savedPost = postRepository.save(reply);
    return savedPost.toPostProfile();
  }

  /**
   * Creates or reactivates a repost entity with the provided input fields using a {@link
   * Transactional} view, ensuring for atomicity and that dirty checking applies.
   *
   * <p>A repost can only be created if the original post is active.
   *
   * @param sharedPostId unique identifier of the shared post
   * @param authorId unique uuid of the authenticated user
   * @return the created repost
   * @throws PostNotFoundException if the shared post cannot be found with {@link
   *     PostRepository#findActivePostById(UUID)}
   * @throws DuplicateRepostException if there is an existing active repost with a matching {@code
   *     sharedPostId} and {@code authorId}
   */
  @Transactional
  public PostProfile createRepost(UUID sharedPostId, UUID authorId) {
    Optional<Post> sharedPost = postRepository.findActivePostById(sharedPostId);
    if (sharedPost.isEmpty()) {
      throw new PostNotFoundException("Original post cannot be found");
    }
    Optional<Post> repost = postRepository.findRepost(sharedPostId, authorId);
    if (repost.isPresent()) {
      if (repost.get().getStatus() == Status.DELETED) {
        // Existing repost needs to be reactivated
        repost.get().setStatus(Status.ACTIVE);
        return repost.get().toPostProfile();
      }
      // Existing repost found; status is active
      throw new DuplicateRepostException(
          String.format(
              "Repost already exists for sharedPostId: %s and authorId: %s",
              sharedPostId, authorId));
    }
    // No existing repost found; create new
    Post newRepost = new Post();
    newRepost.setAuthorId(authorId);
    newRepost.setSharedPostId(sharedPostId);
    Post savedPost = postRepository.save(newRepost);
    return savedPost.toPostProfile();
  }

  /**
   * Creates a quote entity with the provided input fields using a {@link Transactional} view,
   * ensuring for atomicity and that dirty checking applies.
   *
   * <p>A quote can only be created if the original post is active.
   *
   * @param input DTO with post details to be created
   * @param authorId unique uuid of the authenticated user
   * @return the created quote
   * @throws PostNotFoundException if the shared post cannot be found with {@link
   *     PostRepository#findActivePostById(UUID)}
   */
  @Transactional
  public PostProfile createQuote(@Valid CreateQuoteInput input, UUID authorId) {
    Optional<Post> sharedPost = postRepository.findActivePostById(input.sharedPostId());
    if (sharedPost.isEmpty()) {
      throw new PostNotFoundException("Shared post cannot be found");
    }

    Post quote = new Post();
    quote.setAuthorId(authorId);
    quote.setMessageContent(input.messageContent());
    quote.setSharedPostId(input.sharedPostId());
    Post savedPost = postRepository.save(quote);
    return savedPost.toPostProfile();
  }

  /**
   * Updates the post entity with the provided input fields using a {@link Transactional} view,
   * ensuring for atomicity and that dirty checking applies.
   *
   * <p>Only the author of a post can update the post
   *
   * @param input DTO with post details to be updated
   * @param userId unique uuid of the authenticated user
   * @return post with the relevant fields updated
   * @throws NotPostAuthorException when the author of the post does not match the userId parameter
   * @throws PostNotFoundException when post cannot be found in the database
   */
  @Transactional
  public PostProfile updatePost(@Valid UpdatePostInput input, UUID userId) {
    Post post =
        postRepository
            .findById(input.postId())
            .orElseThrow(() -> new PostNotFoundException("Post does not exist"));

    if (!userId.equals(post.getAuthorId())) {
      throw new NotPostAuthorException("Only the author can update the post");
    }

    post.setMessageContent(input.messageContent());

    return post.toPostProfile();
  }

  /**
   * Soft deletes the post by marking their status as {@link Status#DELETED}. Relies on JPA dirty
   * checking within the transaction — no explicit {@code save()} is needed.
   *
   * <p>Only the author of a post can delete the post
   *
   * @param postId unique identifier of the post to be soft-deleted
   * @param userId unique uuid of the authenticated user
   * @throws NotPostAuthorException when the author of the post does not match the userId parameter
   * @throws PostNotFoundException when post cannot be found in the database
   */
  @Transactional
  public void deletePost(UUID postId, UUID userId) {
    Post post =
        postRepository
            .findById(postId)
            .orElseThrow(() -> new PostNotFoundException("Post does not exist"));

    if (!userId.equals(post.getAuthorId())) {
      throw new NotPostAuthorException("Only the author can delete the post");
    }

    post.setStatus(Status.DELETED);
  }

  /**
   * Fetches a paginated list of posts where the queried id is the author.
   *
   * @param authorId unique identifier of the user who authored the posts
   * @param first desired number of results
   * @param after optional cursor of where the previous pagination finished
   * @return a paginated connection of posts, sorted by creation date (descending)
   */
  public PostConnection getActivePosts(UUID authorId, Integer first, String after) {
    Slice<Post> posts;
    Pageable pageable = Pageable.ofSize(first);
    if (after == null) {
      posts = postRepository.findFirstPageOfUsersPosts(authorId, pageable);
    } else {
      Cursor cursor = Cursor.toCursor(after);
      posts =
          postRepository.findNextPageOfUsersPosts(
              authorId, cursor.id(), cursor.timestamp(), pageable);
    }

    return toPostConnection(posts);
  }

  /**
   * Bulk soft deletes all posts authored by the given user by setting their status to {@link
   * Status#DELETED}. Executes as a single JPQL update and joins the caller's transaction.
   *
   * @param authorId unique identifier of the author whose posts will be soft deleted
   */
  public void softDeleteAllByUserId(UUID authorId) {
    postRepository.softDeleteAllByUserId(authorId);
  }
}
