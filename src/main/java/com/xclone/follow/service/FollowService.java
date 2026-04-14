package com.xclone.follow.service;

import com.xclone.common.connection.Cursor;
import com.xclone.common.connection.PageInfo;
import com.xclone.exception.custom.AccountNotActiveException;
import com.xclone.exception.custom.DuplicateFollowException;
import com.xclone.exception.custom.SelfFollowException;
import com.xclone.follow.model.FollowConstraintName;
import com.xclone.follow.model.entity.Follow;
import com.xclone.follow.model.enums.FollowSide;
import com.xclone.follow.repository.FollowRepository;
import com.xclone.user.dto.UserProfile;
import com.xclone.user.dto.connection.UserConnection;
import com.xclone.user.dto.connection.UserEdge;
import com.xclone.user.model.entity.User;
import com.xclone.user.model.enums.UserStatus;
import com.xclone.user.repository.UserRepository;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.postgresql.util.PSQLException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Coordinates resolver logic between the JPA entity layer and the GraphQL API contract. */
@Slf4j
@Service
public class FollowService {

  private final FollowRepository followRepository;
  private final UserRepository userRepository;

  public FollowService(FollowRepository followRepository, UserRepository userRepository) {
    this.followRepository = followRepository;
    this.userRepository = userRepository;
  }

  private UserConnection toUserConnection(Slice<Follow> follows, FollowSide side) {
    List<UserEdge> edges =
        follows.stream()
            .map(
                follow -> {
                  User user =
                      side == FollowSide.FOLLOWER ? follow.getFollower() : follow.getFollowing();
                  Cursor cursor = new Cursor(follow.getCreatedAt(), follow.getId());
                  return new UserEdge(user.toUserProfile(), cursor.encode());
                })
            .toList();
    PageInfo pageInfo =
        new PageInfo(
            follows.hasNext(),
            follows.hasPrevious(),
            edges.isEmpty() ? null : edges.getFirst().cursor(),
            edges.isEmpty() ? null : edges.getLast().cursor());
    return new UserConnection(edges, pageInfo);
  }

  /**
   * Fetches a paginated list of all the followers of a user's account.
   *
   * <p>To find all the accounts which follow the user, the user's id has to be the following id.
   *
   * @param followingId unique user id of the user == follow.following
   * @param first desired number of results
   * @param after optional cursor of where the previous pagination finished
   * @return a list of users sorted by the creation date of the follow relationship
   */
  @Transactional
  public UserConnection getFollowers(UUID followingId, Integer first, String after) {
    Pageable pageable = PageRequest.ofSize(first);
    Slice<Follow> followers;
    if (after == null) {
      followers = followRepository.findFirstPageOfFollowers(followingId, pageable);
    } else {
      Cursor cursor = Cursor.toCursor(after);
      followers =
          followRepository.findNextPageOfFollowers(
              followingId, cursor.id(), cursor.createdAt(), pageable);
    }
    return toUserConnection(followers, FollowSide.FOLLOWER);
  }

  public long getFollowerCount(UUID id) {
    return followRepository.countByFollowing_Id(id);
  }

  /**
   * Fetches a paginated list of all the account which the user follows.
   *
   * <p>To find all the accounts which the user follows, the user's id has to be the follower id.
   *
   * @param followerId unique user id of the user == follow.follower
   * @param first desired number of results
   * @param after optional cursor of where the previous pagination finished
   * @return a list of users sorted by the creation date of the follow relationship
   */
  @Transactional
  public UserConnection getFollowing(UUID followerId, Integer first, String after) {
    Pageable pageable = PageRequest.ofSize(first);
    Slice<Follow> followings;
    if (after == null) {
      followings = followRepository.findFirstPageOfFollowing(followerId, pageable);
    } else {
      Cursor cursor = Cursor.toCursor(after);
      followings =
          followRepository.findNextPageOfFollowing(
              followerId, cursor.id(), cursor.createdAt(), pageable);
    }
    return toUserConnection(followings, FollowSide.FOLLOWING);
  }

  public long getFollowingCount(UUID id) {
    return followRepository.countByFollower_Id(id);
  }

  /**
   * Fetches user entity for provided userId.
   *
   * @param userId unique identifier of user to find
   * @return active user entity
   * @throws UsernameNotFoundException for when there is no data for the queried userId
   */
  private User getUserOrThrow(UUID userId) {
    User user =
        userRepository
            .findById(userId)
            .orElseThrow(
                () -> {
                  log.warn("User with id {} does not exist", userId);
                  return new UsernameNotFoundException("User with specified id does not exist");
                });
    return user;
  }

  /**
   * Fetches user entity for provided userId.
   *
   * <p>Only returns a user if account is {@link UserStatus#ACTIVE}
   *
   * @param userId unique identifier of user to find
   * @return active user entity
   * @throws UsernameNotFoundException for when there is no data for the queried userId
   * @throws AccountNotActiveException if the fetched user entity is not {@link UserStatus#ACTIVE}
   */
  private User getActiveUserOrThrow(UUID userId) {
    User user = getUserOrThrow(userId);
    if (user.getStatus() != UserStatus.ACTIVE) {
      log.warn("User with id {} is not an active account", userId);
      throw new AccountNotActiveException("User account with specified id is not active");
    }
    return user;
  }

  /**
   * Adds a new follow entity to the follow table.
   *
   * @param followerId unique identifier of the user which initiated the follow
   * @param followingId unique identifier of the user which the follow acts upon
   * @return the profile of the followed user
   */
  @Transactional
  public UserProfile followUser(UUID followerId, UUID followingId) {
    try {
      User follower = getActiveUserOrThrow(followerId);
      User following = getActiveUserOrThrow(followingId);
      Follow follow = new Follow();
      follow.setFollower(follower);
      follow.setFollowing(following);
      followRepository.saveAndFlush(follow);
      return following.toUserProfile();
    } catch (DataIntegrityViolationException ex) {
      if (ex.contains(PSQLException.class)) {
        PSQLException psql = (PSQLException) ex.getRootCause();
        String constraintName = psql.getServerErrorMessage().getConstraint();
        if (constraintName.equals(FollowConstraintName.FOLLOW_EXISTS)) {
          throw new DuplicateFollowException("Follow already exists");
        } else if (constraintName.equals(FollowConstraintName.SELF_FOLLOW)) {
          throw new SelfFollowException("User cannot follow self");
        }
      }
      throw ex;
    }
  }

  /**
   * Deletes the follow entity from the follow table.
   *
   * @param followerId unique identifier of the user who is unfollowing
   * @param followingId unique identifier of the user which the unfollow acts upon
   * @return the profile of the unfollowed user
   */
  @Transactional
  public UserProfile unfollowUser(UUID followerId, UUID followingId) {
    followRepository.deleteByFollowerIdAndFollowingId(followerId, followingId);
    return getUserOrThrow(followingId).toUserProfile();
  }

  /**
   * Queries the follow table determine which of the given users the authenticated user is
   * following.
   *
   * @param userId unique identifier of the authenticated user
   * @param users a list of users to check whether the authenticated user follows
   * @return a set of user ids of accounts which the authenticated user follows
   */
  public Set<UUID> getFollowingIdsInUsers(UUID userId, List<UserProfile> users) {
    List<UUID> idsToCheck = users.stream().map(UserProfile::id).toList();
    return new HashSet<>(followRepository.findFollowingIdsInList(userId, idsToCheck));
  }

  /**
   * Retrieves all user ids that the queried user id is following.
   *
   * @param followerId unique identifier of follower in the {@link Follow} relationship
   * @return list of user ids
   */
  public List<UUID> getFollowingIds(UUID followerId) {
    return followRepository.findFollowingIdsByFollowerId(followerId);
  }
}
