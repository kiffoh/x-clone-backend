package com.xclone.like.service;

import com.xclone.common.connection.Cursor;
import com.xclone.exception.custom.NotPostAuthorException;
import com.xclone.like.dto.LikeCount;
import com.xclone.like.repository.LikeRepository;
import com.xclone.post.dto.PostProfile;
import com.xclone.user.dto.connection.UserConnection;
import com.xclone.user.model.entity.User;
import com.xclone.user.service.UserService;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;

/** Performs the business logic to interact with the repository. */
@Service
public class LikeService {

  private final LikeRepository likeRepository;

  public LikeService(LikeRepository likeRepository) {
    this.likeRepository = likeRepository;
  }

  public List<LikeCount> getAllLikeCounts(List<UUID> postIds) {
    return likeRepository.findTotalLikesByPostIds(postIds);
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
    Slice<User> usersThatLikedPost;
    Pageable pageable = Pageable.ofSize(first);

    if (after == null) {
      usersThatLikedPost = likeRepository.findFirstPageOfUsersThatLikedPost(post.id(), pageable);
    } else {
      Cursor cursor = Cursor.toCursor(after);

      usersThatLikedPost =
          likeRepository.findNextPageOfUsersThatLikedPost(
              post.id(), cursor.createdAt(), cursor.id(), pageable);
    }
    return UserService.toUserConnection(usersThatLikedPost);
  }
}
