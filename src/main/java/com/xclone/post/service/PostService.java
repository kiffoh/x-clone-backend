package com.xclone.post.service;

import com.xclone.common.connection.Cursor;
import com.xclone.common.connection.PageInfo;
import com.xclone.follow.repository.FollowRepository;
import com.xclone.post.dto.PostProfile;
import com.xclone.post.dto.connection.PostConnection;
import com.xclone.post.dto.connection.PostEdge;
import com.xclone.post.model.entity.Post;
import com.xclone.post.repository.PostRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;

/** Service layer responsible for post-related operations. */
@Service
public class PostService {
  private final PostRepository postRepository;
  private final FollowRepository followRepository;

  public PostService(PostRepository postRepository, FollowRepository followRepository) {
    this.postRepository = postRepository;
    this.followRepository = followRepository;
  }

  private PostConnection toPostConnection(Slice<Post> posts) {
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

  public PostProfile getPost(UUID id) {
    Optional<Post> post = postRepository.findById(id);
    return post.map(Post::toPostProfile).orElse(null);
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
   * @param userId unique UUID for user entity
   * @param first desired number of results
   * @param after optional cursor of where the previous pagination finished
   * @return a list of posts sorted by creation date
   */
  public PostConnection getFeed(UUID userId, Integer first, String after) {
    Pageable pageable = Pageable.ofSize(first);
    List<UUID> followingIds = followRepository.findFollowingIdsByFollowerId(userId);
    Slice<Post> feed;
    if (after == null) {
      feed = postRepository.findFirstPageOfFeed(userId, followingIds, pageable);
    } else {
      Cursor cursor = Cursor.toCursor(after);
      feed =
          postRepository.findNextPageOfFeed(
              userId, followingIds, cursor.id(), cursor.createdAt(), pageable);
    }
    return toPostConnection(feed);
  }
}
