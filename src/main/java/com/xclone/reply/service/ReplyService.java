package com.xclone.reply.service;

import com.xclone.common.connection.Cursor;
import com.xclone.common.enums.Status;
import com.xclone.post.dto.PostProfile;
import com.xclone.post.dto.connection.PostConnection;
import com.xclone.post.model.entity.Post;
import com.xclone.post.repository.PostRepository;
import com.xclone.post.service.PostService;
import com.xclone.reply.dto.ReplyCount;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
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

  // Should I include all the replies before my reply in the chain?
  // Is this what backwards pagination is for?

  /**
   * Filters the reply thread to fetch all parent posts and direct replies at the same level as the
   * queried {@code postId} which were created before the queried post.
   *
   * @param replyThread a list of all replies in the reply thread sorted by creation date
   *     ascendingly
   * @param postId unique identifier of the queried post
   * @return a list of posts sorted by creation date ascendingly
   */
  private List<PostProfile> filterThread(List<Post> replyThread, UUID postId) {
    Map<UUID, Post> idToPost =
        replyThread.stream().collect(Collectors.toMap(Post::getId, Function.identity()));
    UUID parentId = idToPost.get(postId).getParentId();
    List<Post> filteredThread = new ArrayList<>();
    filteredThread.addAll(getParentsThread(postId, idToPost));
    filteredThread.addAll(getOtherSameLevelReplies(replyThread, postId, parentId));
    return filteredThread.stream()
        .map(post -> post.getStatus() == Status.ACTIVE ? post.toPostProfile() : null)
        .toList();
  }

  /**
   * Filters the reply thread to fetch all other direct replies in the same level as the queried
   * {@code postId}.
   *
   * <p>Stops the process at the point of the queried {@code postId}.
   *
   * @param replyThread a list of all replies in the reply thread sorted by creation date
   *     ascendingly
   * @param postId unique identifier of the post to stop at
   * @param parentId unique identifier of the parent post
   * @return a list of posts sorted by creation date ascendingly
   */
  private List<Post> getOtherSameLevelReplies(List<Post> replyThread, UUID postId, UUID parentId) {
    List<Post> replies = new ArrayList<>();
    for (Post p : replyThread) {
      if (p.getParentId().equals(parentId)) {
        replies.add(p);
        if (p.getId().equals(postId)) {
          break;
        }
      }
    }
    return replies;
  }

  /**
   * Filters the given reply thread to the original post down to the parent of the queried {@code
   * childId}.
   *
   * @param childId unique identifier of the reply chain final child
   * @param idToPost map of a post UUID to the corresponding post
   * @return a list of posts sorted from the original post with each sequential child post
   */
  private List<Post> getParentsThread(UUID childId, Map<UUID, Post> idToPost) {
    Deque<Post> parentsThread = new ArrayDeque<>();
    Post curr = idToPost.get(childId);
    parentsThread.addFirst(curr);

    do {
      parentsThread.addFirst(curr);
      curr = idToPost.getOrDefault(curr.getParentId(), null);
    } while (curr != null);
    return parentsThread.stream().toList();
  }

  /**
   * Fetches reply thread slice for all posts which are created earlier than the queried {@code
   * postId}.
   *
   * @param replyThreadId unique identifier for all posts in a reply thread
   * @param postId unique identifier of the queried post
   * @return a filtered list of posts sorted by creation date ascendingly
   */
  public List<PostProfile> getReplyThread(UUID replyThreadId, UUID postId) {
    List<Post> replyThread = postRepository.findAllByReplyThreadIdSortByCreatedAtAsc(replyThreadId);
    return filterThread(replyThread, postId);
  }
}
