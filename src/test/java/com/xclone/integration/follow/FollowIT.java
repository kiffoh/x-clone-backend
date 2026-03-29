package com.xclone.integration.follow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.xclone.common.connection.Cursor;
import com.xclone.follow.repository.FollowRepository;
import com.xclone.integration.base.BaseIntegrationTest;
import com.xclone.support.fixtures.UserFixtures;
import com.xclone.support.helpers.AuthHelpers;
import com.xclone.user.dto.connection.UserConnection;
import com.xclone.user.dto.mutation.UserResponse;
import com.xclone.user.model.entity.User;
import com.xclone.user.repository.UserRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
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
    void validInput_returnsUserProfile() {
      User authenticatedUser = users.getFirst();
      // Not the authenticated user
      User userToFollow = users.get(1);

      authenticatedTester
          .document(
              """
                  mutation FollowUser($id: ID!) {
                    followUser(userIdToFollow: $id) {
                      code
                      success
                      user {
                        id
                        followers {
                          edges {
                            node {
                              id
                            }
                          }
                        }
                      }
                    }
                  }
                  """)
          .variable("id", userToFollow.getId())
          .execute()
          // Assert
          .path("followUser")
          .matchesJson(
              String.format(
                  """
               {
                    "code": "201",
                    "success": true,
                    "user": {
                      "id": "%s",
                      "followers": {
                        "edges": [{
                          "node": {
                            "id": "%s"
                           }
                        }]
                      }
                    }
                  }
              """,
                  userToFollow.getId(), authenticatedUser.getId()));

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
                        "id": "%s",
                        "following": {
                          "edges": [{
                            "node": {
                              "id": "%s"
                             }
                          }]
                        }
                      }""",
                  authenticatedUser.getId(), userToFollow.getId()));
    }

    @Test
    void existingFollow_returnsFieldError() {
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
    void followSelf_returnsFieldError() {
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
    void invalidUUID_returnsProtocolError() {
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
          .errors()
          .filter(error -> "BAD_REQUEST".equals(error.getExtensions().get("classification")))
          .expect(error -> error.getMessage().contains("not a valid UUID"));
    }

    @Test
    void invalidUserId_returnsFieldError() {
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
              .variable("id", UUID.randomUUID().toString())
              .execute()
              .path("followUser")
              .entity(UserResponse.class)
              .get();

      assertFalse(followResponse.success());
      assertThat(followResponse.code()).isEqualTo("404");
      assertThat(followResponse.errors()).hasSize(1);
      assertThat(followResponse.errors().getFirst().field()).isEqualTo("userIdToFollow");
      assertThat(followResponse.errors().getFirst().message())
          .isEqualTo("User with specified id does not exist");
      assertNull(followResponse.user());
    }

    @Test
    void paginationWithValidAfter_returnsNextUserConnection() {
      User userToFollow1 = users.get(1);
      User userToFollow2 = users.get(2);

      // Follow 2 users
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
          .variable("id", userToFollow1.getId())
          .execute()
          .path("followUser")
          .matchesJson(
              """
               {
                  "code": "201",
                  "success": true
               }
              """);
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
          .variable("id", userToFollow2.getId())
          .execute()
          .path("followUser")
          .matchesJson(
              """
               {
                  "code": "201",
                  "success": true
               }
              """);

      UserConnection firstPage =
          authenticatedTester
              .document(
                  """
                  {
                    me {
                      following(first: 1) {
                        edges {
                          node {
                            id
                          }
                          cursor
                        }
                        pageInfo {
                          hasNextPage
                          hasPreviousPage
                          endCursor
                        }
                      }
                      followingCount
                    }
                  }
                  """)
              .execute()
              .path("me.followingCount")
              .entity(Long.class)
              .isEqualTo(2L)
              .path("me.following")
              .entity(UserConnection.class)
              .get();

      String firstPageEndCursor = firstPage.pageInfo().endCursor();

      // Followings are sorted by most recent -> second follow will be the first page
      assertThat(firstPage.edges().getFirst().node().id()).isEqualTo(userToFollow2.getId());
      assertThat(firstPage.edges().getFirst().cursor()).isEqualTo(firstPageEndCursor);
      assertTrue(firstPage.pageInfo().hasNextPage());
      assertFalse(firstPage.pageInfo().hasPreviousPage());

      UserConnection secondPage =
          authenticatedTester
              .document(
                  """
                  query getFollowing($after: String) {
                    me {
                      following(first: 1, after: $after) {
                        edges {
                          node {
                            id
                          }
                          cursor
                        }
                        pageInfo {
                          hasNextPage
                          hasPreviousPage
                          endCursor
                        }
                      }
                      followingCount
                    }
                  }
                  """)
              .variable("after", firstPageEndCursor)
              .execute()
              .path("me.followingCount")
              .entity(Long.class)
              .isEqualTo(2L)
              .path("me.following")
              .entity(UserConnection.class)
              .get();

      assertThat(secondPage.edges().getFirst().node().id()).isEqualTo(userToFollow1.getId());
      assertThat(secondPage.edges().getFirst().cursor())
          .isEqualTo(secondPage.pageInfo().endCursor());
      assertFalse(secondPage.pageInfo().hasNextPage());
    }

    @Test
    void paginationWithMalformedAfter_returnsProtocolError() {
      String malformedAfter = "not-base64!";
      // Act
      authenticatedTester
          .document(
              """
                  query getFollowing($after: String) {
                    me {
                      following(first: 1, after: $after) {
                        edges {
                          node {
                            id
                          }
                        }
                      }
                    }
                  }
                  """)
          .variable("after", malformedAfter)
          .execute()
          .errors()
          .filter(error -> "BAD_REQUEST".equals(error.getExtensions().get("classification")))
          .expect(error -> error.getMessage().contains("Malformed cursor"));
    }

    @Test
    void paginationWithValidAfterButNoData_returnsEmptyConnection() {
      String cursorPastEnd =
          new Cursor(Instant.now().minus(1, ChronoUnit.HOURS), UUID.randomUUID()).encode();

      // One follower in DB
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
          .variable("id", users.get(1).getId())
          .execute()
          .path("followUser")
          .matchesJson(
              """
               {
                  "code": "201",
                  "success": true
               }
              """);

      // Act
      UserConnection emptyData =
          authenticatedTester
              .document(
                  """
                  query getFollowing($after: String) {
                    me {
                      following(first: 1, after: $after) {
                        edges {
                          node {
                            id
                          }
                          cursor
                        }
                        pageInfo {
                          hasNextPage
                        }
                      }
                      followingCount
                    }
                  }
                  """)
              .variable("after", cursorPastEnd)
              .execute()
              .path("me.followingCount")
              .entity(Long.class)
              .isEqualTo(1L)
              .path("me.following")
              .entity(UserConnection.class)
              .get();

      // Assert
      assertThat(emptyData.edges()).hasSize(0);
      assertFalse(emptyData.pageInfo().hasNextPage());
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
    void validInput_returnsUserProfile() {
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
                            "id": "%s"
                          }
                          """,
                      authenticatedUser.getId()))
              .path("me.following")
              .entity(UserConnection.class)
              .get();

      assertThat(following.edges()).hasSize(0);
    }

    @Test
    void idempotentUnfollow_returnsUserProfile() {
      // Unfollow to establish conditions
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

      // Act idempotent unfollow and assert
      authenticatedTester
          .document(
              """
                  mutation UnfollowUser($id: ID!) {
                    unfollowUser(userIdToUnfollow: $id) {
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
          .variable("id", userToUnfollow.getId())
          .execute()
          .path("unfollowUser")
          .matchesJson(
              String.format(
                  """
                  {
                    "code": "200",
                    "success": true,
                    "user": {
                      "id": "%s"
                    },
                    "errors": null
                  }
                  """,
                  userToUnfollow.getId()));
    }

    @Test
    void invalidUUID_returnsProtocolError() {
      authenticatedTester
          .document(
              """
                  mutation UnfollowUser($id: ID!) {
                    unfollowUser(userIdToUnfollow: $id) {
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
          .errors()
          .filter(error -> "BAD_REQUEST".equals(error.getExtensions().get("classification")))
          .expect(error -> error.getMessage().contains("not a valid UUID"));
    }

    @Test
    void invalidUserId_returnsFieldError() {
      // Act and assert
      UserResponse unfollowResponse =
          authenticatedTester
              .document(
                  """
                  mutation UnfollowUser($id: ID!) {
                    unfollowUser(userIdToUnfollow: $id) {
                      code
                      success
                      errors {
                        field
                        message
                      }
                    }
                  }
                  """)
              .variable("id", UUID.randomUUID().toString())
              .execute()
              .path("unfollowUser")
              .entity(UserResponse.class)
              .get();

      assertFalse(unfollowResponse.success());
      assertThat(unfollowResponse.code()).isEqualTo("404");
      assertThat(unfollowResponse.errors()).hasSize(1);
      assertThat(unfollowResponse.errors().getFirst().field()).isEqualTo("userIdToUnfollow");
      assertThat(unfollowResponse.errors().getFirst().message())
          .isEqualTo("User with specified id does not exist");
      assertNull(unfollowResponse.user());
    }
  }
}
