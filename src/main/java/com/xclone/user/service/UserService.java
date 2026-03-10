package com.xclone.user.service;

import com.xclone.common.connection.PageInfo;
import com.xclone.exception.custom.DuplicateHandleException;
import com.xclone.user.dto.UserProfile;
import com.xclone.user.dto.connection.UserConnection;
import com.xclone.user.dto.connection.UserEdge;
import com.xclone.user.dto.request.UpdateUserRequest;
import com.xclone.user.model.entity.User;
import com.xclone.user.model.enums.UserStatus;
import com.xclone.user.repository.UserRepository;
import com.xclone.validation.ValidHandle;
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

  //   Is request better than input?
  public UserProfile updateProfile(User user, @Valid UpdateUserRequest updateUserInput) {
    if (updateUserInput.bio() != null) {
      user.setBio(updateUserInput.bio());
    }
    if (updateUserInput.displayName() != null) {
      user.setDisplayName(updateUserInput.displayName());
    }
    // Do I need to check for handle?
    if (updateUserInput.handle() != null) {
      if (userRepository.existsByHandle(updateUserInput.handle())) {
        log.warn("signup attempt with an existing handle");
        throw new DuplicateHandleException("This handle is already taken");
      } else {
        user.setHandle(updateUserInput.handle());
      }
    }
    if (updateUserInput.profileImage() != null) {
      user.setProfileImage(updateUserInput.profileImage());
    }
    User updatedUser = userRepository.save(user);
    return updatedUser.toUserProfile();
  }

  public void deleteProfile(User user) {
    user.setStatus(UserStatus.DELETED);
  }
}
