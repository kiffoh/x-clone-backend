package com.xclone.user.service;

import com.xclone.common.connection.Cursor;
import com.xclone.common.connection.PageInfo;
import com.xclone.exception.custom.DuplicateHandleException;
import com.xclone.follow.service.FollowService;
import com.xclone.user.dto.UserProfile;
import com.xclone.user.dto.connection.UserConnection;
import com.xclone.user.dto.connection.UserEdge;
import com.xclone.user.dto.request.UpdateUserInput;
import com.xclone.user.model.entity.User;
import com.xclone.user.model.enums.UserStatus;
import com.xclone.user.repository.UserRepository;
import com.xclone.validation.ValidHandle;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

/** Coordinates resolver logic for the User GraphQL model. */
@Slf4j
@Service
@Validated
public class UserService {
  private final UserRepository userRepository;

  private final FollowService followService;

  public UserService(UserRepository userRepository, FollowService followService) {
    this.userRepository = userRepository;
    this.followService = followService;
  }

  /**
   * Builds a {@link UserConnection} from a flat list of {@link User} entities. {@link
   * PageInfo#hasNextPage()} and {@link PageInfo#hasPreviousPage()} are always {@code false} until
   * cursor pagination is implemented.
   *
   * @param users the users to wrap; may be empty
   * @return a connection containing edges, page metadata, and total count
   */
  public static UserConnection toUserConnection(Slice<User> users) {
    List<UserEdge> edges =
        users.stream()
            .map(
                user -> {
                  Cursor cursor = new Cursor(user.getUpdatedAt(), user.getId());
                  return new UserEdge(user.toUserProfile(), cursor.encode());
                })
            .toList();
    PageInfo pageInfo =
        new PageInfo(
            users.hasNext(),
            users.hasPrevious(),
            edges.isEmpty() ? null : edges.getFirst().cursor(),
            edges.isEmpty() ? null : edges.getLast().cursor());
    return new UserConnection(edges, pageInfo);
  }

  public UserProfile getUserByHandle(@ValidHandle String handle) {
    Optional<User> user = userRepository.findByHandle(handle);
    return user.map(User::toUserProfile).orElse(null);
  }

  public UserProfile getUserById(UUID id) {
    Optional<User> user = userRepository.findById(id);
    return user.map(User::toUserProfile).orElse(null);
  }

  /**
   * Fetches user profiles from input list which have an active status.
   *
   * @param userIds list of user ids to query
   * @return list of user profiles which have an active status
   */
  public List<UserProfile> getActiveUsersById(List<UUID> userIds) {
    return userRepository.findAllActiveUsersByIdIn(userIds).stream()
        .map(User::toUserProfile)
        .toList();
  }

  /**
   * Fetches a paginated of accounts whose handles contain the given query string.
   *
   * @param query the substring to search for within user handles
   * @param first desired number of results
   * @param after optional cursor of where the previous pagination finished
   * @return a list of users sorted by the creation date of the user's account
   */
  public UserConnection getUsersByHandle(String query, Integer first, String after) {
    Pageable pageable = PageRequest.ofSize(first);
    Slice<User> users;
    if (after == null) {
      users = userRepository.findAllByHandleContainingOrderByCreatedAtDescIdAsc(query, pageable);
    } else {
      Cursor cursor = Cursor.toCursor(after);
      users =
          userRepository.findAllByHandleContainingNextPage(
              query, cursor.id(), cursor.createdAt(), pageable);
    }
    return toUserConnection(users);
  }

  /**
   * Updates the user entity with the provided input fields using a {@link Transactional} view,
   * ensuring for an accurate and consistent view of the user entity. {@link
   * DuplicateHandleException} throws when the handle in {@link UpdateUserInput} already exists in
   * the database and is not the current user handle.
   *
   * @param userId unique UUID for user entity
   * @param updateUserInput DTO with user profile fields to be updated
   * @return user with relevant fields updated
   */
  @Transactional
  public UserProfile updateProfile(UUID userId, @Valid UpdateUserInput updateUserInput) {
    User user =
        userRepository
            .findById(userId)
            .orElseThrow(
                () ->
                    new IllegalStateException(
                        "Authenticated user not found in database: " + userId));
    if (updateUserInput.bio() != null) {
      user.setBio(updateUserInput.bio());
    }
    if (updateUserInput.displayName() != null) {
      user.setDisplayName(updateUserInput.displayName());
    }
    // Do I need to check for handle?
    if (updateUserInput.handle() != null) {
      if (userRepository.existsByHandleAndIdNot(updateUserInput.handle(), user.getId())) {
        log.debug("update profile attempt with an existing handle");
        throw new DuplicateHandleException("This handle is already taken");
      } else {
        user.setHandle(updateUserInput.handle());
      }
    }
    if (updateUserInput.profileImage() != null) {
      user.setProfileImage(updateUserInput.profileImage());
    }
    return user.toUserProfile();
  }

  /**
   * Soft deletes the authenticated user by marking their status as {@link UserStatus#DELETED}.
   * Relies on JPA dirty checking within the transaction — no explicit {@code save()} is needed.
   *
   * @param userId unique UUID for user entity
   * @throws IllegalStateException if the authenticated user cannot be found in the database,
   *     indicating a mismatch between the security context and the persisted state
   */
  @Transactional
  public void deleteProfile(UUID userId) {
    User user =
        userRepository
            .findById(userId)
            .orElseThrow(
                () ->
                    new IllegalStateException(
                        "Authenticated user not found in database: " + userId));
    user.setStatus(UserStatus.DELETED);
  }

  /**
   * Fetches a paginated list of accounts that the authenticated user does not follow.
   *
   * <p>The result excludes:
   *
   * <ul>
   *   <li>Users already followed by the authenticated user (users following)
   *   <li>The authenticated user themselves
   * </ul>
   *
   * @param followerId unique UUID for user entity
   * @param first desired number of results
   * @param after optional cursor of where the previous pagination finished
   * @return a list of users sorted by the creation date of the user's account
   */
  public UserConnection getSuggestedUsers(UUID followerId, Integer first, String after) {
    Pageable pageable = PageRequest.ofSize(first);
    List<UUID> userIdsToExclude = followService.getFollowingIds(followerId);
    userIdsToExclude.add(followerId);

    Slice<User> users;
    if (after == null) {
      users = userRepository.findAllByIdNotIn(userIdsToExclude, pageable);
    } else {
      Cursor cursor = Cursor.toCursor(after);
      users =
          userRepository.findAllByIdNotInNext(
              userIdsToExclude, cursor.id(), cursor.createdAt(), pageable);
    }
    return toUserConnection(users);
  }
}
