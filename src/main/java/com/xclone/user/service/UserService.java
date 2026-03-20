package com.xclone.user.service;

import com.xclone.common.connection.PageInfo;
import com.xclone.exception.custom.DuplicateHandleException;
import com.xclone.user.dto.UserProfile;
import com.xclone.user.dto.connection.UserConnection;
import com.xclone.user.dto.connection.UserEdge;
import com.xclone.user.dto.request.UpdateUserInput;
import com.xclone.user.model.entity.User;
import com.xclone.user.model.enums.UserStatus;
import com.xclone.user.repository.UserRepository;
import com.xclone.validation.ValidHandle;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

/** Coordinates resolver logic for the User GraphQL model. */
@Slf4j
@Service
@Validated
public class UserService {
  private final UserRepository userRepository;

  public UserService(UserRepository userRepository) {
    this.userRepository = userRepository;
  }

  /**
   * Builds a {@link UserConnection} from a flat list of {@link User} entities. {@link
   * PageInfo#hasNextPage()} and {@link PageInfo#hasPreviousPage()} are always {@code false} until
   * cursor pagination is implemented.
   *
   * @param users the users to wrap; may be empty
   * @return a connection containing edges, page metadata, and total count
   */
  public static UserConnection toUserConnection(List<User> users) {
    List<UserEdge> edges =
        users.stream()
            .map(user -> new UserEdge(user.toUserProfile(), user.getId().toString()))
            .toList();
    PageInfo pageInfo =
        new PageInfo(
            false,
            false,
            edges.isEmpty() ? null : edges.getFirst().cursor(),
            edges.isEmpty() ? null : edges.getLast().cursor());
    return new UserConnection(edges, pageInfo, edges.size());
  }

  public UserProfile getUserByHandle(@ValidHandle String handle) {
    Optional<User> user = userRepository.findByHandle(handle);
    return user.map(User::toUserProfile).orElse(null);
  }

  public UserProfile getUserById(UUID id) {
    Optional<User> user = userRepository.findById(id);
    return user.map(User::toUserProfile).orElse(null);
  }

  public UserConnection getUsersByHandle(String query) {
    List<User> users = userRepository.findAllByHandleContaining(query);
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
  public UserProfile updateProfile(String userId, @Valid UpdateUserInput updateUserInput) {
    User user =
        userRepository
            .findById(UUID.fromString(userId))
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
  public void deleteProfile(String userId) {
    User user =
        userRepository
            .findById(UUID.fromString(userId))
            .orElseThrow(
                () ->
                    new IllegalStateException(
                        "Authenticated user not found in database: " + userId));
    user.setStatus(UserStatus.DELETED);
  }
}
