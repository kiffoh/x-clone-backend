package com.xclone.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.xclone.exception.custom.DuplicateHandleException;
import com.xclone.integration.validation.ValidationIT;
import com.xclone.support.fixtures.UserFixtures;
import com.xclone.user.dto.UserProfile;
import com.xclone.user.dto.connection.UserConnection;
import com.xclone.user.dto.request.UpdateUserInput;
import com.xclone.user.model.entity.User;
import com.xclone.user.repository.UserRepository;
import com.xclone.validation.ObjectNotEmpty;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * The unit testing for the following functions has been skipped as they are thin services and
 * testing would only be testing the framework:
 *
 * <ul>
 *   <li>me
 *   <li>getUserByHandle
 *   <li>getUsersByHandle
 *   <li>deleteProfile
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
public class UserServiceTest {
  @Mock UserRepository userRepository;

  @InjectMocks UserService userService;

  /** Tests getUsersByHandle to confirm UserConnection mapping */
  @Nested
  class getUsersByHandleTests {
    @Test
    public void getUsersByHandle_multipleUsers_returnsUserConnection() {
      String query = "exam";
      List<String> handles = List.of("exampleHandle", "exampleHandle1", "exampleHandle2");
      List<User> generatedUsers =
          handles.stream()
              .map(
                  handle -> {
                    User user = UserFixtures.getDefaultUserWithRandomId();
                    user.setHandle(handle);
                    user.setId(UUID.randomUUID());
                    return user;
                  })
              .toList();
      List<UUID> generatedUserIds = generatedUsers.stream().map(User::getId).toList();

      when(userRepository.findAllByHandleContaining(query)).thenReturn(generatedUsers);

      UserConnection returnedUsers = userService.getUsersByHandle(query);
      List<UUID> returnedUserIds =
          returnedUsers.edges().stream().map(user -> user.node().id()).toList();

      assertThat(returnedUsers.totalCount()).isEqualTo(3);
      // Edges
      assertThat(returnedUserIds).isEqualTo(generatedUserIds);
      // Page info
      assertThat(returnedUsers.pageInfo().startCursor())
          .isEqualTo(generatedUserIds.getFirst().toString());
      assertThat(returnedUsers.pageInfo().endCursor())
          .isEqualTo(generatedUserIds.getLast().toString());
      assertThat(returnedUsers.pageInfo().hasNextPage()).isFalse();
      assertThat(returnedUsers.pageInfo().hasPreviousPage()).isFalse();
    }

    @Test
    public void getUsersByHandle_singleUser_returnsUserConnection() {
      String query = "exampleHandle1";
      List<String> handles = List.of("exampleHandle1");
      List<User> generatedUser =
          handles.stream()
              .map(
                  handle -> {
                    User user = UserFixtures.getDefaultUserWithRandomId();
                    user.setHandle(handle);
                    return user;
                  })
              .toList();
      UUID firstUserId = generatedUser.getFirst().getId();
      when(userRepository.findAllByHandleContaining(query)).thenReturn(generatedUser);

      UserConnection returnedUsers = userService.getUsersByHandle(query);

      assertThat(returnedUsers.totalCount()).isEqualTo(1);
      // Edges
      assertThat(returnedUsers.edges().getFirst().node().id()).isEqualTo(firstUserId);
      // Page info
      assertThat(returnedUsers.pageInfo().startCursor()).isEqualTo(firstUserId.toString());
      assertThat(returnedUsers.pageInfo().endCursor()).isEqualTo(firstUserId.toString());
      assertThat(returnedUsers.pageInfo().hasNextPage()).isFalse();
      assertThat(returnedUsers.pageInfo().hasPreviousPage()).isFalse();
    }

    @Test
    public void getUsersByHandle_noUsers_returnsUserConnection() {
      String query = "random";
      List<User> generatedUsers = List.of();
      when(userRepository.findAllByHandleContaining(query)).thenReturn(generatedUsers);

      UserConnection returnedUsers = userService.getUsersByHandle(query);

      assertThat(returnedUsers.totalCount()).isEqualTo(0);
      // Page info
      assertThat(returnedUsers.pageInfo().startCursor()).isNull();
      assertThat(returnedUsers.pageInfo().endCursor()).isNull();
      assertThat(returnedUsers.pageInfo().hasNextPage()).isFalse();
      assertThat(returnedUsers.pageInfo().hasPreviousPage()).isFalse();
    }
  }

  /**
   * The test where all {@link UpdateUserInput} values as null has been skipped. It is a duplication
   * of the {@link ObjectNotEmpty} annotation tests in {@link ValidationIT}.
   */
  @Nested
  class updateProfileTests {

    User existingUser;
    String existingUserId;

    @BeforeEach
    void initialisation() {
      existingUser = UserFixtures.getDefaultUserWithRandomId();
      existingUserId = existingUser.getId().toString();
    }

    @Test
    void updatesSingleField_returnsUserProfile() {
      UpdateUserInput input = new UpdateUserInput("NewName", null, null, null);
      String originalDisplayName = existingUser.getDisplayName();

      when(userRepository.findById(existingUser.getId())).thenReturn(Optional.of(existingUser));
      UserProfile returnedUser = userService.updateProfile(existingUserId, input);

      assertThat(returnedUser.displayName()).isNotEqualTo(originalDisplayName);
      assertThat(returnedUser).isEqualTo(existingUser.toUserProfile());
    }

    @Test
    void updatesUserHandle_newHandle_returnsUserProfile() {
      String newHandle = "new";
      UpdateUserInput input = new UpdateUserInput(null, newHandle, null, null);
      String originalHandle = existingUser.getHandle();

      when(userRepository.findById(existingUser.getId())).thenReturn(Optional.of(existingUser));
      when(userRepository.existsByHandleAndIdNot(newHandle, existingUser.getId()))
          .thenReturn(false);

      UserProfile returnedUser = userService.updateProfile(existingUserId, input);

      assertThat(returnedUser.handle()).isNotEqualTo(originalHandle);
      assertThat(returnedUser).isEqualTo(existingUser.toUserProfile());
    }

    @Test
    void updatesAllFields_returnsUserProfile() {
      String newHandle = "new";
      UpdateUserInput input =
          new UpdateUserInput(
              "NewName",
              newHandle,
              "This is an updated Bio",
              "https://www.thisisanewprofileuri.com");
      User originalUser = existingUser.toBuilder().build();

      when(userRepository.findById(existingUser.getId())).thenReturn(Optional.of(existingUser));
      when(userRepository.existsByHandleAndIdNot(newHandle, existingUser.getId()))
          .thenReturn(false);

      UserProfile returnedUser = userService.updateProfile(existingUserId, input);

      assertThat(returnedUser.displayName()).isNotEqualTo(originalUser.getDisplayName());
      assertThat(returnedUser.handle()).isNotEqualTo(originalUser.getHandle());
      assertThat(returnedUser.bio()).isNotEqualTo(originalUser.getBio());
      assertThat(returnedUser.profileImage()).isNotEqualTo(originalUser.getProfileImage());
      assertThat(returnedUser).isEqualTo(existingUser.toUserProfile());
    }

    @Test
    void updatesUserHandle_userSubmitsCurrentHandleAsUpdateInput_returnsUserProfile() {
      String existingHandle = existingUser.getHandle();
      UpdateUserInput input = new UpdateUserInput(null, existingHandle, null, null);

      when(userRepository.findById(existingUser.getId())).thenReturn(Optional.of(existingUser));
      when(userRepository.existsByHandleAndIdNot(existingHandle, existingUser.getId()))
          .thenReturn(false);

      UserProfile returnedUser = userService.updateProfile(existingUserId, input);

      assertThat(returnedUser).isEqualTo(existingUser.toUserProfile());
    }

    @Test
    void updatesUserHandle_existingHandle_returnsDuplicateHandle() {
      String existingHandle = "existing";
      UpdateUserInput input = new UpdateUserInput(null, existingHandle, null, null);

      when(userRepository.findById(existingUser.getId())).thenReturn(Optional.of(existingUser));
      when(userRepository.existsByHandleAndIdNot(existingHandle, existingUser.getId()))
          .thenReturn(true);
      assertThatThrownBy(() -> userService.updateProfile(existingUserId, input))
          .isInstanceOf(DuplicateHandleException.class)
          .hasMessage("This handle is already taken");
    }

    @Test
    void invalidUserId_returnsIllegalState() {
      UpdateUserInput input = new UpdateUserInput("NewName", null, null, null);

      when(userRepository.findById(existingUser.getId())).thenReturn(Optional.empty());

      assertThatThrownBy(() -> userService.updateProfile(existingUserId, input))
          .isInstanceOf(IllegalStateException.class)
          .hasMessage("Authenticated user not found in database: " + existingUserId);
    }
  }
}
