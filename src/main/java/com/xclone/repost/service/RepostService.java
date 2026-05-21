package com.xclone.repost.service;

import com.xclone.common.connection.Cursor;
import com.xclone.post.dto.PostProfile;
import com.xclone.post.dto.connection.PostConnection;
import com.xclone.post.model.entity.Post;
import com.xclone.post.repository.PostRepository;
import com.xclone.post.service.PostService;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;

/** Service layer responsible for repost-related operations. */
@Service
public class RepostService {
  private final PostRepository postRepository;

  public RepostService(PostRepository postRepository) {
    this.postRepository = postRepository;
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
}
