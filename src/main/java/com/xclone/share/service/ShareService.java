package com.xclone.share.service;

import com.xclone.common.connection.Cursor;
import com.xclone.common.connection.PageInfo;
import com.xclone.post.dto.PostProfile;
import com.xclone.post.dto.connection.PostConnection;
import com.xclone.post.model.entity.Post;
import com.xclone.post.repository.PostRepository;
import com.xclone.post.service.PostService;
import com.xclone.share.dto.ShareCount;
import com.xclone.user.dto.connection.UserConnection;
import com.xclone.user.dto.connection.UserEdge;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;

/** Service layer responsible for share-related operations. */
@Service
public class ShareService {
  private final PostRepository postRepository;

  public ShareService(PostRepository postRepository) {
    this.postRepository = postRepository;
  }

  private UserConnection toUserConnection(Slice<Post> reposts) {
    List<UserEdge> edges =
        reposts.stream()
            .map(
                repost -> {
                  Cursor cursor = new Cursor(repost.getCreatedAt(), repost.getId());
                  return new UserEdge(repost.getAuthor().toUserProfile(), cursor.encode());
                })
            .toList();
    PageInfo pageInfo =
        new PageInfo(
            reposts.hasNext(),
            reposts.hasPrevious(),
            edges.isEmpty() ? null : edges.getFirst().cursor(),
            edges.isEmpty() ? null : edges.getLast().cursor());
    return new UserConnection(edges, pageInfo);
  }

  /**
   * Fetches a paginated list of posts which are direct quotes to the queried post.
   *
   * @param quotedPostId unique identifier of the parent post
   * @param first desired number of results
   * @param after optional cursor of where the previous pagination finished
   * @return a list of posts sorted by creation date
   */
  public PostConnection getQuotes(UUID quotedPostId, Integer first, String after) {
    Slice<Post> quotes;
    Pageable pageable = Pageable.ofSize(first);

    if (after == null) {
      quotes = postRepository.findFirstPageOfQuotes(quotedPostId, pageable);
    } else {
      Cursor cursor = Cursor.toCursor(after);
      quotes =
          postRepository.findNextPageOfQuotes(
              quotedPostId, cursor.createdAt(), cursor.id(), pageable);
    }
    return PostService.toPostConnection(quotes);
  }

  /**
   * Gets the quoted post for each quote entity.
   *
   * @param quotes list of posts which may be quote entities
   * @return list of quoted posts for each quote entity
   */
  public List<PostProfile> getQuotedPosts(List<PostProfile> quotes) {
    List<UUID> quotedPostIds =
        quotes.stream().map(PostProfile::quotedPostId).filter(Objects::nonNull).toList();
    if (quotedPostIds.isEmpty()) {
      return List.of();
    }
    List<Post> quotedPosts = postRepository.findQuotedPosts(quotedPostIds);
    return quotedPosts.stream().map(Post::toPostProfile).toList();
  }

  /**
   * Fetches a paginated list of users who reposted the queried post.
   *
   * @param quotedPostId unique identifier of the shared post
   * @param first desired number of results
   * @param after optional cursor of where the previous pagination finished
   * @return a list of users sorted by the repost creation date
   */
  public UserConnection getRepostUsers(UUID quotedPostId, Integer first, String after) {
    Slice<Post> reposts;
    Pageable pageable = Pageable.ofSize(first);

    if (after == null) {
      reposts = postRepository.findFirstPageOfPureReposts(quotedPostId, pageable);
    } else {
      Cursor cursor = Cursor.toCursor(after);
      reposts =
          postRepository.findNextPageOfPureReposts(
              quotedPostId, cursor.createdAt(), cursor.id(), pageable);
    }
    return toUserConnection(reposts);
  }

  /**
   * Fetches the amount of shares each post has.
   *
   * <p>Triggers {@link PostRepository#findShareCounts(List)}.
   *
   * @param postIds unique identifiers of posts
   * @return a list of {@link ShareCount} DTOs which contain the post id and its respective share
   *     count.
   */
  public List<ShareCount> getShareCounts(List<UUID> postIds) {
    return postRepository.findShareCounts(postIds);
  }

  /**
   * Identifies which of the post ids the authenticated user has shared.
   *
   * <p>Triggers {@link PostRepository#findSharedIds(List, UUID)} and returns the output as a Set.
   * The set removes duplicates for a case where a user might have reposted and quoted the same
   * post.
   *
   * @param postIds unique identifiers of posts
   * @param userId unique identifier of the authenticated user
   * @return the post ids which the user has shared in a set
   */
  public Set<UUID> getSharedIdsInPosts(List<UUID> postIds, UUID userId) {
    return new HashSet<>(postRepository.findSharedIds(postIds, userId));
  }
}
