package com.xclone.like.service;

import com.xclone.common.connection.Cursor;
import com.xclone.common.connection.PageInfo;
import com.xclone.exception.custom.NotPostAuthorException;
import com.xclone.exception.custom.PostNotFoundException;
import com.xclone.like.dto.LikeCount;
import com.xclone.like.model.LikeConstraintName;
import com.xclone.like.model.entity.Like;
import com.xclone.like.repository.LikeRepository;
import com.xclone.post.dto.PostProfile;
import com.xclone.user.dto.connection.UserConnection;
import com.xclone.user.dto.connection.UserEdge;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.postgresql.util.PSQLException;
import org.postgresql.util.ServerErrorMessage;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Performs the business logic to interact with the repository. */
@Slf4j
@Service
public class LikeService {

  private final LikeRepository likeRepository;

  public LikeService(LikeRepository likeRepository) {
    this.likeRepository = likeRepository;
  }

  private UserConnection toUserConnection(Slice<Like> likes) {
    List<UserEdge> edges =
        likes.stream()
            .map(
                like -> {
                  Cursor cursor = new Cursor(like.getCreatedAt(), like.getId());
                  return new UserEdge(like.getUser().toUserProfile(), cursor.encode());
                })
            .toList();
    PageInfo pageInfo =
        new PageInfo(
            likes.hasNext(),
            likes.hasPrevious(),
            edges.isEmpty() ? null : edges.getFirst().cursor(),
            edges.isEmpty() ? null : edges.getLast().cursor());
    return new UserConnection(edges, pageInfo);
  }

  public List<LikeCount> getLikeCounts(List<UUID> postIds) {
    return likeRepository.findActiveLikesByPostIds(postIds);
  }

  public Set<UUID> getPostIdsThatUserLikes(List<UUID> postIds, UUID userId) {
    return new HashSet<>(likeRepository.findPostIdsThatUserLikes(postIds, userId));
  }

  /**
   * Fetches a paginated list of active users which have liked the queried post.
   *
   * @param userId unique identifier of the authenticated user
   * @param post queried post which has potential likes
   * @param first desired number of results
   * @param after optional cursor of where the previous pagination finished
   * @return a list of users sorted by creation date of the like
   * @throws NotPostAuthorException if the queried user id does not match the posts author id
   */
  public UserConnection getUsersThatLikedPost(
      UUID userId, PostProfile post, Integer first, String after) {
    if (!userId.equals(post.authorId())) {
      throw new NotPostAuthorException("Only a post author can view a posts likes");
    }
    Slice<Like> likes;
    Pageable pageable = Pageable.ofSize(first);

    if (after == null) {
      likes = likeRepository.findFirstPageOfUsersThatLikedPost(post.id(), pageable);
    } else {
      Cursor cursor = Cursor.toCursor(after);

      likes =
          likeRepository.findNextPageOfUsersThatLikedPost(
              post.id(), cursor.createdAt(), cursor.id(), pageable);
    }
    return toUserConnection(likes);
  }

  /**
   * Creates a like entity and inserts it into the database.
   *
   * <p>Transactional approach is omitted as {@code saveAndFlush} manages its own transaction.
   *
   * @param postId unique identifier of post to like
   * @param userId unique identifier of user that liked the post
   */
  public void createLike(UUID postId, UUID userId) {
    try {
      Like like = new Like();
      like.setPostId(postId);
      like.setUserId(userId);
      likeRepository.saveAndFlush(like);
    } catch (DataIntegrityViolationException ex) {
      if (ex.contains(PSQLException.class)) {
        PSQLException psql = (PSQLException) ex.getRootCause();
        if (psql != null) {
          ServerErrorMessage serverError = psql.getServerErrorMessage();
          if (serverError != null) {
            String constraintName = serverError.getConstraint();
            if (constraintName != null) {
              if (constraintName.equals(LikeConstraintName.LIKE_EXISTS)) {
                // Swallow duplicate like as idempotent behaviour is acceptable
                log.debug("Like already exists");
                return;
              }
              if (constraintName.equals(LikeConstraintName.LIKE_POST_FK)) {
                throw new PostNotFoundException("Post does not exist");
              }
            }
          }
        }
      }
      throw ex;
    }
  }

  /**
   * Removes a like from the database with the queried parameters.
   *
   * @param postId unique identifier of post to unlike
   * @param userId unique identifier of user that unliked the post
   */
  @Transactional
  public void deleteLike(UUID postId, UUID userId) {
    likeRepository.deleteLikeByPostIdAndUserId(postId, userId);
  }
}
