package com.xclone.reply.service;

import com.xclone.common.connection.Cursor;
import com.xclone.common.connection.PageInfo;
import com.xclone.post.dto.PostProfile;
import com.xclone.post.dto.connection.PostConnection;
import com.xclone.post.dto.connection.PostEdge;
import com.xclone.post.model.entity.Post;
import com.xclone.post.repository.PostRepository;
import com.xclone.reply.dto.ReplyCount;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;

/** Performs business logic for replies to interact with the post repository. */
@Service
public class ReplyService {
  private final PostRepository postRepository;

  public ReplyService(PostRepository postRepository) {
    this.postRepository = postRepository;
  }

  private PostConnection toPostConnection(Slice<Post> replies) {
    List<PostEdge> edges =
        replies.stream()
            .map(
                comment -> {
                  Cursor cursor = new Cursor(comment.getCreatedAt(), comment.getId());
                  return new PostEdge(comment.toPostProfile(), cursor.encode());
                })
            .toList();
    PageInfo pageInfo =
        new PageInfo(
            replies.hasNext(),
            replies.hasPrevious(),
            edges.isEmpty() ? null : edges.getFirst().cursor(),
            edges.isEmpty() ? null : edges.getLast().cursor());
    return new PostConnection(edges, pageInfo);
  }

  public List<PostProfile> getPostParents(List<UUID> parentIds) {
    return postRepository.findAllActiveParents(parentIds).stream()
        .map(Post::toPostProfile)
        .toList();
  }

  /**
   * Obtains the count for the direct replies for each queried post id.
   *
   * @param postIds list of unique identifiers for each parent post.
   * @return a list of each post and its reply count
   */
  public List<ReplyCount> getReplyCounts(List<UUID> postIds) {
    return postRepository.findAllReplyCountsByParentIds(postIds);
  }

  /**
   * Fetches a paginated list of posts which are direct replies to the queried post.
   *
   * @param postId unique identifier of the parent post
   * @param first desired number of results
   * @param after optional cursor of where the previous pagination finished TODO: update javadoc
   *     when sorting algo is updated
   * @return a list of posts sorted by creation date
   */
  public PostConnection getReplies(UUID postId, Integer first, String after) {
    Slice<Post> replies;
    Pageable pageable = Pageable.ofSize(first);

    if (after == null) {
      replies = postRepository.findFirstPageOfReplies(postId, pageable);
    } else {
      Cursor cursor = Cursor.toCursor(after);
      replies =
          postRepository.findNextPageOfReplies(postId, cursor.createdAt(), cursor.id(), pageable);
    }
    return toPostConnection(replies);
  }
}
