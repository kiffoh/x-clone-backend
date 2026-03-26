package com.xclone.integration.follow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.xclone.follow.repository.FollowRepository;
import com.xclone.integration.base.BaseIntegrationTest;
import com.xclone.support.fixtures.UserFixtures;
import com.xclone.support.helpers.AuthHelpers;
import com.xclone.user.dto.connection.UserConnection;
import com.xclone.user.dto.mutation.UserResponse;
import com.xclone.user.model.entity.User;
import com.xclone.user.repository.UserRepository;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.graphql.tester.AutoConfigureHttpGraphQlTester;
import org.springframework.context.annotation.Import;
import org.springframework.graphql.test.tester.HttpGraphQlTester;

@AutoConfigureHttpGraphQlTester
@Import(AuthHelpers.class)
public class FollowIT extends BaseIntegrationTest {
  @Autowired UserRepository userRepository;
  @Autowired FollowRepository followRepository;
  @Autowired AuthHelpers authHelpers;
  @Autowired HttpGraphQlTester authenticatedTester;

  List<String> handles = List.of("example1", "example2", "example3");

  List<User> users;

  @BeforeEach
  void setup() {
    // Flushes DB
    userRepository.deleteAll();
    // Adds 3 users to the DB under the handles
    users =
        handles.stream().map(UserFixtures::createUserWithHandle).map(userRepository::save).toList();
    // Sets the accessToken to match that of the first user
    String accessToken = authHelpers.getUserAccessToken(users.getFirst().getId().toString());
    authenticatedTester =
        authenticatedTester.mutate().headers(headers -> headers.setBearerAuth(accessToken)).build();
  }

  @AfterEach
  void cleanup() {
    followRepository.deleteAll();
  }

  @Nested
  class followUserTests {
    @Test
    void followUser_returnsUserProfile() {
      User authenticatedUser = users.getFirst();
      // Not the authenticated user
      User userToFollow = users.get(1);

      UserResponse followResponse =
          authenticatedTester
              .document(
                  """
              mutation FollowUser($id: ID!) {
                followUser(userIdToFollow: $id) {
                  code
                  success
                }
              }
              """)
              .variable("id", userToFollow.getId())
              .execute()
              .path("followUser")
              .entity(UserResponse.class)
              .get();

      authenticatedTester
          .document(
              """
              {
                me {
                  id
                  following(first: 5) {
                    edges {
                      node {
                        id
                      }
                    }
                    totalCount
                  }
                }
              }
              """)
          .execute()
          .path("me")
          .matchesJson(
              String.format(
                  """
                      {
                        "id": "%s",
                        "following": {
                          "edges": [{
                            "node": {
                              "id": %s
                             }
                          }],
                          "totalCount": 1
                        }
                      }""",
                  authenticatedUser.getId(), userToFollow.getId()));

      assertThat(followResponse.code()).isEqualTo("201");
      assertTrue(followResponse.success());
    }

    @Test
    void followUser_existingFollow_returnsFieldError() {
      User userToFollow = users.get(1);
      authenticatedTester
          .document(
              """
              mutation FollowUser($id: ID!) {
                followUser(userIdToFollow: $id) {
                  code
                  success
                }
              }
              """)
          .variable("id", userToFollow.getId())
          .execute()
          .path("followUser")
          .matchesJson(
              """
              {
                "code": "201",
                "success": true
              }
              """);

      UserResponse existingFollow =
          authenticatedTester
              .document(
                  """
              mutation FollowUser($id: ID!) {
                followUser(userIdToFollow: $id) {
                  code
                  success
                  user {
                    id
                  }
                  errors {
                    field
                    message
                  }
                }
              }
              """)
              .variable("id", userToFollow.getId())
              .execute()
              .path("followUser")
              .entity(UserResponse.class)
              .get();

      assertThat(existingFollow.code()).isEqualTo("409");
      assertFalse(existingFollow.success());
      assertThat(existingFollow.errors()).hasSize(1);
      assertThat(existingFollow.errors().getFirst().field()).isEqualTo("userIdToFollow");
      assertThat(existingFollow.errors().getFirst().message()).isEqualTo("Follow already exists");
      assertNull(existingFollow.user());
    }

    @Test
    void followUser_followSelf_returnsFieldError() {
      User authenticatedUser = users.getFirst();

      UserResponse followResponse =
          authenticatedTester
              .document(
                  """
              mutation FollowUser($id: ID!) {
                followUser(userIdToFollow: $id) {
                  code
                  success
                  user {
                    id
                  }
                  errors {
                    field
                    message
                  }
                }
              }
              """)
              .variable("id", authenticatedUser.getId())
              .execute()
              .path("followUser")
              .entity(UserResponse.class)
              .get();

      assertFalse(followResponse.success());
      assertThat(followResponse.code()).isEqualTo("400");
      assertThat(followResponse.errors()).hasSize(1);
      assertThat(followResponse.errors().getFirst().field()).isEqualTo("userIdToFollow");
      assertThat(followResponse.errors().getFirst().message()).isEqualTo("User cannot follow self");
      assertNull(followResponse.user());
    }

    @Test
    void followUser_invalidUUID_returnsFieldError() {
      UserResponse followResponse =
          authenticatedTester
              .document(
                  """
              mutation FollowUser($id: ID!) {
                followUser(userIdToFollow: $id) {
                  code
                  success
                  user {
                    id
                  }
                  errors {
                    field
                    message
                  }
                }
              }
              """)
              .variable("id", "not a valid UUID")
              .execute()
              .path("followUser")
              .entity(UserResponse.class)
              .get();

      assertFalse(followResponse.success());
      assertThat(followResponse.code()).isEqualTo("400");
      assertThat(followResponse.errors()).hasSize(1);
      assertThat(followResponse.errors().getFirst().field()).isEqualTo("userIdToFollow");
      assertThat(followResponse.errors().getFirst().message())
          .isEqualTo("Invalid UUID string: not a valid UUID");
      assertNull(followResponse.user());
    }
  }

  @Nested
  class unfollowUserTests {
    User authenticatedUser;
    // Not the authenticated user
    User userToUnfollow;

    @BeforeEach
    void setup() {
      authenticatedUser = users.getFirst();
      userToUnfollow = users.get(1);

      // Initialise follower as part of setup
      authenticatedTester
          .document(
              """
              mutation FollowUser($id: ID!) {
                followUser(userIdToFollow: $id) {
                  code
                  success
                }
              }
              """)
          .variable("id", userToUnfollow.getId())
          .execute()
          .path("followUser")
          .matchesJson(
              """
              {
                "code": "201",
                "success": true
              }
              """);
    }

    @Test
    void unfollowUser_returnsUserProfile() {
      // Act and assert
      authenticatedTester
          .document(
              """
              mutation UnfollowUser($id: ID!) {
                unfollowUser(userIdToUnfollow: $id) {
                  code
                  success
                }
              }
              """)
          .variable("id", userToUnfollow.getId())
          .execute()
          .path("unfollowUser")
          .matchesJson(
              """
              {
                "code": "200",
                "success": true
              }
              """);

      UserConnection following =
          authenticatedTester
              .document(
                  """
              {
                me {
                  id
                  following(first: 5) {
                    edges {
                      node {
                        id
                      }
                    }
                  }
                }
              }
              """)
              .execute()
              .path("me")
              .matchesJson(
                  String.format(
                      """
              {
                "id": %s
              }
              """,
                      authenticatedUser.getId()))
              .path("me.following")
              .entity(UserConnection.class)
              .get();

      assertThat(following.edges()).hasSize(0);
    }
  }
}
