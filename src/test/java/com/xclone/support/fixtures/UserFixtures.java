package com.xclone.support.fixtures;

import com.xclone.user.dto.connection.UserConnection;
import com.xclone.user.model.entity.User;
import com.xclone.user.service.UserService;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class UserFixtures {
  public static final UUID DEFAULT_USER_ID =
      UUID.fromString("00000000-0000-0000-0000-000000000001");

  public static User getDefaultUserWithRandomId() {
    User user = new User();
    user.setHandle("exampleHandle");
    user.setPasswordHash("hashedPassword");
    user.setDisplayName("displayName");
    user.setCreatedAt(Instant.now());
    user.setUpdatedAt(Instant.now());
    user.setId(UUID.randomUUID());
    return user;
  }

  public static User createUserWithHandle(String handle) {
    User user = new User();
    user.setHandle(handle);
    user.setPasswordHash("hashedPassword");
    user.setDisplayName("displayName");
    user.setCreatedAt(Instant.now());
    user.setUpdatedAt(Instant.now());
    return user;
  }

  public static User getDefaultUserWithStaticId() {
    User user = new User();
    user.setHandle("exampleHandle");
    user.setPasswordHash("hashedPassword");
    user.setDisplayName("displayName");
    user.setCreatedAt(Instant.now());
    user.setUpdatedAt(Instant.now());
    user.setId(DEFAULT_USER_ID);
    return user;
  }

  public static UserConnection getDefaultUserConnection() {
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
    return UserService.toUserConnection(generatedUsers);
  }
}
