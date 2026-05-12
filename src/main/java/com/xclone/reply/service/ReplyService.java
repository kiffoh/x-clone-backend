package com.xclone.reply.service;

import com.xclone.common.connection.Cursor;
import com.xclone.post.dto.PostProfile;
import com.xclone.post.dto.connection.PostConnection;
import com.xclone.post.model.entity.Post;
import com.xclone.post.repository.PostRepository;
import com.xclone.post.service.PostService;
import com.xclone.reply.dto.ReplyCount;
import com.xclone.reply.dto.ReplyThread;
import java.util.List;
import java.util.Optional;
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

  /**
   * Fetches the {@link PostProfile} for a valid and active parent.
   *
   * <p>Returns null for posts with no parent, e.g. the original post in a reply chain.
   *
   * @param parentId unique identifier of the parent post
   * @return a post
   */
  public PostProfile getParent(UUID parentId) {
    Optional<Post> post = postRepository.findActivePostById(parentId);
    return post.map(Post::toPostProfile).orElse(null);
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
    return PostService.toPostConnection(replies);
  }

  /**
   * Fetches reply thread slice for all posts which are created earlier than the queried {@code
   * postId}.
   *
   * @param post starting post in a reply thread
   * @return a 2D list of posts, one of ancestors and one of siblings, both sorted by creation date
   *     ascendingly
   */
  public ReplyThread getReplyThread(PostProfile post) {
    List<Post> ancestors = postRepository.findAllAncestors(post.id());
    List<Post> filteredAncestors =
        ancestors.stream().filter(ancestor -> !(ancestor.getId().equals(post.id()))).toList();
    List<Post> siblings =
        postRepository.findAllSiblings(post.parentId(), post.createdAt().toInstant());
    return new ReplyThread(
        filteredAncestors.stream().map(Post::toPostProfile).toList(),
        siblings.stream().map(Post::toPostProfile).toList(),
        post);
  }
}
