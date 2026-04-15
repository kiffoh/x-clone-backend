package com.xclone.integration.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.xclone.common.connection.Cursor;
import com.xclone.common.mutation.DeleteResponse;
import com.xclone.exception.dto.FieldError;
import com.xclone.follow.repository.FollowRepository;
import com.xclone.integration.base.BaseIntegrationTest;
import com.xclone.post.dto.connection.PostEdge;
import com.xclone.post.model.entity.Post;
import com.xclone.post.repository.PostRepository;
import com.xclone.support.fixtures.UserFixtures;
import com.xclone.support.helpers.AuthHelpers;
import com.xclone.support.helpers.FollowHelpers;
import com.xclone.support.helpers.PostHelpers;
import com.xclone.user.dto.connection.UserConnection;
import com.xclone.user.dto.mutation.UserResponse;
import com.xclone.user.model.entity.User;
import com.xclone.user.model.enums.UserStatus;
import com.xclone.user.repository.UserRepository;
import com.xclone.validation.ValidationConstants;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.graphql.tester.AutoConfigureHttpGraphQlTester;
import org.springframework.context.annotation.Import;
import org.springframework.graphql.ResponseError;
import org.springframework.graphql.test.tester.HttpGraphQlTester;

@AutoConfigureHttpGraphQlTester
@Import(AuthHelpers.class)
public class UserIT extends BaseIntegrationTest {

  @Autowired UserRepository userRepository;
  @Autowired AuthHelpers authHelpers;
  @Autowired HttpGraphQlTester graphQlTester;

  List<String> handles = List.of("example1", "example2", "example3", "example4");

  List<User> users;

  String accessToken;

  @BeforeEach
  void setup() {
    // Flushes DB
    userRepository.deleteAll();
    // Adds 3 users to the DB under the handles
    users =
        handles.stream().map(UserFixtures::createUserWithHandle).map(userRepository::save).toList();
    // Sets the accessToken to match that of the first user
    accessToken = authHelpers.getUserAccessToken(users.getFirst().getId().toString());
  }

  // Helpers
  private HttpGraphQlTester authenticatedTester() {
    return graphQlTester.mutate().headers(headers -> headers.setBearerAuth(accessToken)).build();
  }

  /**
   * Unauthenticated rejection not tested here — covered globally in {@link
   * com.xclone.integration.security.SecurityIT}
   */
  @Nested
  class meTests {
    @Test
    void me_returnsUser() {
      User authenticatedUser = users.getFirst();
      authenticatedTester()
          .document(
              """
                  {
                    me {
                      handle
                      id
                    }
                   }
                  """)
          .execute()
          .path("me.handle")
          .entity(String.class)
          .isEqualTo(authenticatedUser.getHandle())
          .path("me.id")
          .entity(String.class)
          .isEqualTo(authenticatedUser.getId().toString());
    }
  }

  @Nested
  class userByHandleTests {
    @Test
    void userByHandle_userExists_returnsUser() {
      String handle = handles.getFirst();
      authenticatedTester()
          .document(
              String.format(
                  """
                      {
                        userByHandle(handle: "%s") {
                          id
                          handle
                        }
                       }
                      """,
                  handle))
          .execute()
          .path("userByHandle.handle")
          .entity(String.class)
          .isEqualTo(handle)
          .path("userByHandle.id")
          .entity(String.class)
          .isEqualTo(users.getFirst().getId().toString());
    }

    @Test
    void userByHandle_userNotExist_returnsNull() {
      String userWithHandleDoesNotExist = "handleNotExist";
      authenticatedTester()
          .document(
              String.format(
                  """
                      {
                        userByHandle(handle: "%s") {
                          id
                          handle
                        }
                       }
                      """,
                  userWithHandleDoesNotExist))
          .execute()
          .path("userByHandle")
          .valueIsNull();
    }

    @Test
    void userByHandle_invalidHandle_returnsValidationViolations() {
      // Invalid chars + size violation
      String invalidHandle = "invalid!AndLongerThan14Chars";
      int numberOfViolations = 2;

      List<ResponseError> errors =
          authenticatedTester()
              .document(
                  String.format(
                      """
                          {
                            userByHandle(handle: "%s") {
                              id
                              handle
                            }
                           }
                          """,
                      invalidHandle))
              .execute()
              .returnResponse()
              .getErrors();

      assertThat(errors).hasSize(numberOfViolations);
      assertThat(errors)
          .extracting(e -> e.getExtensions().get("field"), ResponseError::getMessage)
          .containsExactlyInAnyOrder(
              tuple("handle", ValidationConstants.INVALID_HANDLE_SIZE),
              tuple("handle", ValidationConstants.INVALID_HANDLE_REGEX));
    }
  }

  @Nested
  class userByIdTests {
    /** GraphQL serializes a UUID to a string. */
    @Test
    void userById_userExists_returnsUser() {
      User user = users.getFirst();
      authenticatedTester()
          .document(
              """
                  query UserById($Id: ID!) {
                    userById(id: $Id) {
                      id
                      handle
                    }
                   }
                  """)
          .variable("Id", user.getId())
          .execute()
          .path("userById.handle")
          .entity(String.class)
          .isEqualTo(user.getHandle())
          .path("userById.id")
          .entity(String.class)
          .isEqualTo(user.getId().toString());
    }

    @Test
    void userById_userNotExist_returnsNull() {
      User userDoesNotExist = UserFixtures.getDefaultUserWithRandomId();
      authenticatedTester()
          .document(
              String.format(
                  """
                      {
                        userById(id: "%s") {
                          id
                          handle
                        }
                       }
                      """,
                  userDoesNotExist.getId()))
          .execute()
          .path("userById")
          .valueIsNull();
    }
  }

  @Nested
  class searchUsersTests {
    @Test
    void searchUsers_validQueryForMultipleUser_returnsUserConnection() {
      String query = "exam";
      authenticatedTester()
          .document(
              String.format(
                  """
                      {
                        searchUsers(query: "%s") {
                          edges {
                            node {
                              handle
                            }
                           }
                         }
                      }
                      """,
                  query))
          .execute()
          .path("searchUsers.edges[*].node.handle")
          .entityList(String.class)
          .satisfies(handles -> handles.forEach((handle -> assertThat(handle).contains(query))));
    }

    @Test
    void searchUsers_validQueryForSingleUser_returnsUserConnection() {
      String query = "example1";
      authenticatedTester()
          .document(
              String.format(
                  """
                      {
                        searchUsers(query: "%s") {
                          edges {
                            node {
                              handle
                            }
                           }
                         }
                      }
                      """,
                  query))
          .execute()
          .path("searchUsers.edges[*].node.handle")
          .entityList(String.class)
          .satisfies(handles -> handles.forEach((handle -> assertThat(handle).contains(query))));
    }

    @Test
    void searchUsers_queryWithNoMatch_returnsUserConnection() {
      String query = "noUserHasThisHandle";
      authenticatedTester()
          .document(
              String.format(
                  """
                      {
                        searchUsers(query: "%s") {
                          edges {
                            node {
                              handle
                            }
                           }
                         }
                      }
                      """,
                  query))
          .execute()
          .path("searchUsers.edges[*].node.handle")
          .entityList(String.class)
          .hasSize(0);
    }
  }

  @Nested
  class suggestedUsersTests {
    @Autowired FollowRepository followRepository;

    User authenticatedUser;
    User user2;
    User user3;
    User user4;

    @AfterEach
    void cleanup() {
      followRepository.deleteAll();
    }

    @BeforeEach
    void setup() {
      authenticatedUser = users.getFirst();
      user2 = users.get(1);
      user3 = users.get(2);
      user4 = users.get(3);
    }

    @Test
    void suggestedUsers_moreThanOneUserNotFollowing_returnsUserConnection() {
      // authenticated user only follows user2
      FollowHelpers.seedFollow(followRepository, authenticatedUser, user2);

      List<UUID> response =
          authenticatedTester()
              .document(
                  """
                      {
                        suggestedUsers {
                          edges {
                            node {
                              id
                            }
                           }
                         }
                      }
                      """)
              .execute()
              .path("suggestedUsers.edges[*].node.id")
              .entityList(UUID.class)
              .get();

      assertThat(response).hasSize(2);
      // Self and following are not returned
      assertFalse(response.contains(authenticatedUser.getId()));
      assertFalse(response.contains(user2.getId()));
      // Users that authenticated user does not follow
      assertTrue(response.contains(user3.getId()));
      assertTrue(response.contains(user4.getId()));
    }

    @Test
    void suggestedUsers_oneUserNotFollowing_returnsUserConnection() {
      // authenticated user follows user2 + user3
      FollowHelpers.seedFollow(followRepository, authenticatedUser, user2);
      FollowHelpers.seedFollow(followRepository, authenticatedUser, user3);

      List<UUID> response =
          authenticatedTester()
              .document(
                  """
                      {
                        suggestedUsers {
                          edges {
                            node {
                              id
                            }
                           }
                         }
                      }
                      """)
              .execute()
              .path("suggestedUsers.edges[*].node.id")
              .entityList(UUID.class)
              .get();

      assertThat(response).hasSize(1);
      // Self and following are not returned
      assertFalse(response.contains(authenticatedUser.getId()));
      assertFalse(response.contains(user2.getId()));
      assertFalse(response.contains(user3.getId()));
      // Users that authenticated user does not follow
      assertTrue(response.contains(user4.getId()));
    }

    @Test
    void suggestedUsers_followingAllUsers_returnsEmptyUserConnection() {
      // authenticated user only follows user2
      FollowHelpers.seedFollow(followRepository, authenticatedUser, user2);
      FollowHelpers.seedFollow(followRepository, authenticatedUser, user3);
      FollowHelpers.seedFollow(followRepository, authenticatedUser, user4);

      List<UUID> response =
          authenticatedTester()
              .document(
                  """
                      {
                        suggestedUsers {
                          edges {
                            node {
                              id
                            }
                           }
                         }
                      }
                      """)
              .execute()
              .path("suggestedUsers.edges[*].node.id")
              .entityList(UUID.class)
              .get();

      assertThat(response).hasSize(0);
    }
  }

  @Nested
  class updateMyProfileTests {
    // Confirm DB is updated! Do I do this in another test or the same one?
    @Test
    void updateMyProfile_validInput_allFields_returnsUserResponse() {
      String newDisplayName = "newName";
      String newHandle = "newHandle";
      String bio = "this is my new bio";
      String profileImage = "https://www.linktonewuri.com";

      authenticatedTester()
          .document(
              """
                  mutation UpdateProfile($input: UpdateUserInput!) {
                    updateMyProfile(input: $input) {
                      code
                      success
                      user {
                        displayName
                        handle
                        bio
                        profileImage
                      }
                      errors {
                        field
                        message
                      }
                    }
                  }
                  """)
          .variable(
              "input",
              Map.of(
                  "displayName", newDisplayName,
                  "handle", newHandle,
                  "bio", bio,
                  "profileImage", profileImage))
          .execute()
          .path("updateMyProfile")
          .matchesJson(
              String.format(
                  """
                      {
                        "code": "200",
                        "success": true,
                        "user": {
                          "displayName": "%s",
                          "handle": "%s",
                          "bio": "%s",
                          "profileImage": "%s"
                        },
                        "errors": null
                      }
                      """,
                  newDisplayName, newHandle, bio, profileImage));
    }

    @Test
    void updateMyProfile_validInput_onlyDisplayName_returnsUserResponse() {
      User authenticatedUser = users.getFirst();
      String newDisplayName = "newName";

      UserResponse response =
          authenticatedTester()
              .document(
                  """
                      mutation UpdateProfile($input: UpdateUserInput!) {
                        updateMyProfile(input: $input) {
                          code
                          success
                          user {
                            displayName
                            handle
                            bio
                            profileImage
                          }
                          errors {
                            field
                            message
                          }
                        }
                      }
                      """)
              .variable("input", Map.of("displayName", newDisplayName))
              .execute()
              .path("updateMyProfile")
              .entity(UserResponse.class)
              .get();

      assertEquals("200", response.code());
      assertTrue(response.success());
      assertEquals(newDisplayName, response.user().displayName());
      assertEquals(authenticatedUser.getHandle(), response.user().handle());
      assertNull(response.user().bio());
      assertNull(response.user().profileImage());
      assertNull(response.errors());
    }

    @Test
    void updateMyProfile_invalidInput_returnsInvalidRequest() {
      Map<String, Object> input = new HashMap<>();
      input.put("displayName", "This is an invalid display name");
      input.put("handle", "invalidHandle!!!!!!");

      UserResponse response =
          authenticatedTester()
              .document(
                  """
                      mutation UpdateProfile($input: UpdateUserInput!) {
                        updateMyProfile(input: $input) {
                          code
                          success
                          user {
                            displayName
                            handle
                            bio
                            profileImage
                          }
                          errors {
                            field
                            message
                          }
                        }
                      }
                      """)
              .variable("input", input)
              .execute()
              .path("updateMyProfile")
              .entity(UserResponse.class)
              .get();

      assertEquals("400", response.code());
      assertFalse(response.success());
      assertNull(response.user());
      assertThat(response.errors())
          .extracting(FieldError::field, FieldError::message)
          .containsExactlyInAnyOrder(
              tuple("handle", ValidationConstants.INVALID_HANDLE_SIZE),
              tuple("handle", ValidationConstants.INVALID_HANDLE_REGEX));
    }

    @Test
    void updateMyProfile_usingCurrentHandle_returnsUserResponse() {
      Map<String, Object> input = new HashMap<>();
      User authenticatedUser = users.getFirst();
      String newDisplayName = "newName";
      input.put("displayName", newDisplayName);
      input.put("handle", authenticatedUser.getHandle());

      UserResponse response =
          authenticatedTester()
              .document(
                  """
                      mutation UpdateProfile($input: UpdateUserInput!) {
                        updateMyProfile(input: $input) {
                          code
                          success
                          user {
                            displayName
                            handle
                            bio
                            profileImage
                          }
                          errors {
                            field
                            message
                          }
                        }
                      }
                      """)
              .variable("input", input)
              .execute()
              .path("updateMyProfile")
              .entity(UserResponse.class)
              .get();

      assertEquals("200", response.code());
      assertTrue(response.success());
      assertNull(response.errors());
      assertThat(response.user().handle()).isEqualTo(authenticatedUser.getHandle());
      assertThat(response.user().displayName()).isEqualTo(newDisplayName);
    }

    /**
     * User will not partially update as DuplicateHandle is an unchecked exception (extends
     * RuntimeException) and consequently defaults to rollback.
     */
    @Test
    void updateMyProfile_usingExistingHandle_returnsDuplicateHandle() {
      Map<String, Object> input = new HashMap<>();
      input.put("handle", users.getLast().getHandle());

      UserResponse response =
          authenticatedTester()
              .document(
                  """
                      mutation UpdateProfile($input: UpdateUserInput!) {
                        updateMyProfile(input: $input) {
                          code
                          success
                          user {
                            displayName
                            handle
                            bio
                            profileImage
                          }
                          errors {
                            field
                            message
                          }
                        }
                      }
                      """)
              .variable("input", input)
              .execute()
              .path("updateMyProfile")
              .entity(UserResponse.class)
              .get();

      assertEquals("409", response.code());
      assertFalse(response.success());
      assertNull(response.user());
      assertThat(response.errors())
          .extracting(FieldError::field, FieldError::message)
          .containsExactly(tuple("handle", "This handle is already taken"));
    }
  }

  @Nested
  class deleteMyAccountTests {
    @Test
    void deleteMyAccount_deletesAccount() {
      User authenticatedUser = users.getFirst();

      DeleteResponse response =
          authenticatedTester()
              .document(
                  """
                      mutation DeleteAccount {
                        deleteMyAccount {
                          success
                          code
                          errors {
                            field
                            message
                           }
                        }
                      }
                      """)
              .execute()
              .path("deleteMyAccount")
              .entity(DeleteResponse.class)
              .get();

      User userAfterDelete =
          userRepository
              .findById(authenticatedUser.getId())
              .orElseThrow(() -> new IllegalStateException("User doesn't exist"));

      assertTrue(response.success());
      assertThat(response.code()).isEqualTo("200");
      assertNull(response.errors());
      assertThat(userAfterDelete.getStatus()).isEqualTo(UserStatus.DELETED);
    }
  }

  @Nested
  class postMappingTests {
    @Autowired PostRepository postRepository;

    User authenticatedUser;

    List<Post> posts;
    List<String> messageContents = List.of("one for sorrow", "two for joy", "three for a girl");

    void wipePostDB() {
      postRepository.deleteAll();
    }

    @BeforeEach
    void setup() {
      wipePostDB();
      authenticatedUser = users.getFirst();
    }

    @AfterEach
    void cleanup() {
      // AfterEach required in addition to BeforeEach — post rows must be removed
      // before followMappingTests runs to avoid FK constraint violations.
      // @AfterAll was attempted but requires static fields; @Autowired repositories
      // behave differently when static, resulting in null injection at teardown.
      wipePostDB();
    }

    /** Creates 3 posts with the authenticated user as the author. */
    void createPostsForAuthenticatedUser() {
      List<User> authors = Collections.nCopies(3, authenticatedUser);
      posts = PostHelpers.seedPosts(messageContents, authors, postRepository);
    }

    @Test
    void userHasNoPosts_returnsEmptyPostConnection() {
      authenticatedTester()
          .document(
              """
                  query getPosts {
                    me {
                      posts {
                        edges {
                          node {
                            id
                          }
                        }
                        pageInfo {
                          hasNextPage
                        }
                      }
                    }
                  }
                  """)
          .execute()
          .path("me.posts.edges")
          .entityList(PostEdge.class)
          .hasSize(0)
          .path("me.posts.pageInfo.hasNextPage")
          .entity(Boolean.class)
          .isEqualTo(false);
    }

    @Test
    void userHasPosts_noCursor_returnsPostConnection() {
      createPostsForAuthenticatedUser();

      authenticatedTester()
          .document(
              """
                  query getPosts {
                    me {
                      posts {
                        edges {
                          node {
                            messageContent
                          }
                        }
                      }
                    }
                  }
                  """)
          .execute()
          .path("me.posts.edges[*].node.messageContent")
          .entityList(String.class)
          .satisfies(
              contents -> {
                assertThat(contents).hasSize(3);
                assertThat(contents).containsExactlyInAnyOrderElementsOf(messageContents);
              });
    }

    @Test
    void userHasPosts_paginationWithValidAfter_returnsPostConnection() {
      createPostsForAuthenticatedUser();

      String endCursor =
          authenticatedTester()
              .document(
                  """
                      query getPosts {
                        me {
                          posts(first: 1) {
                            edges {
                              node {
                                messageContent
                              }
                            }
                            pageInfo {
                              endCursor
                            }
                          }
                        }
                      }
                      """)
              .execute()
              .path("me.posts.edges[*].node.messageContent")
              .entityList(String.class)
              .satisfies(
                  contents -> {
                    assertThat(contents).hasSize(1);
                    assertThat(contents.getFirst()).contains(messageContents.getLast());
                  })
              .path("me.posts.pageInfo.endCursor")
              .entity(String.class)
              .get();

      authenticatedTester()
          .document(
              """
                  query getPosts($after: String) {
                    me {
                      posts(first: 2, after: $after) {
                        edges {
                          node {
                            messageContent
                          }
                        }
                        pageInfo {
                          endCursor
                        }
                      }
                    }
                  }
                  """)
          .variable("after", endCursor)
          .execute()
          .path("me.posts.edges[*].node.messageContent")
          .entityList(String.class)
          .satisfies(
              contents -> {
                assertThat(contents).hasSize(2);
                assertThat(contents)
                    .containsExactlyInAnyOrderElementsOf(messageContents.subList(0, 2));
              });
    }

    @Test
    void paginationWithValidAfterButNoData_returnsEmptyConnection() {
      createPostsForAuthenticatedUser();
      // A cursor timestamped 1 hour in the past will exclude posts created moments ago, so the
      // result should be empty
      String cursorPastEnd =
          new Cursor(Instant.now().minus(1, ChronoUnit.HOURS), UUID.randomUUID()).encode();

      // Act
      authenticatedTester()
          .document(
              """
                  query getPosts($after: String) {
                    me {
                      posts(after: $after) {
                        edges {
                          node {
                            id
                          }
                        }
                        pageInfo {
                          hasNextPage
                        }
                      }
                    }
                  }
                  """)
          .variable("after", cursorPastEnd)
          .execute()
          .path("me.posts.edges")
          .entityList(PostEdge.class)
          .hasSize(0)
          .path("me.posts.pageInfo.hasNextPage")
          .entity(Boolean.class)
          .isEqualTo(false);
    }

    @Test
    void paginationWithMalformedAfter_returnsProtocolError() {
      createPostsForAuthenticatedUser();

      String malformedAfter = "not-base64!";
      // Act + Assert
      authenticatedTester()
          .document(
              """
                  query getPosts($after: String) {
                    me {
                      posts(first: 1, after: $after) {
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
  }

  @Nested
  class followMappingTests {
    @Autowired FollowRepository followRepository;

    User authenticatedUser;
    User user2;
    User user3;

    void wipeFollowDB() {
      followRepository.deleteAll();
    }

    @AfterEach
    void cleanup() {
      // AfterEach required in addition to BeforeEach — post rows must be removed
      // before followMappingTests runs to avoid FK constraint violations.
      // @AfterAll was attempted but requires static fields; @Autowired repositories
      // behave differently when static, resulting in null injection at teardown.
      wipeFollowDB();
    }

    @Nested
    class followingTests {
      /**
       * Following system:
       *
       * <p>user 1 follows:
       *
       * <ul>
       *   <li>user 2
       *   <li>user 3
       * </ul>
       */
      @BeforeEach
      void setup() {
        wipeFollowDB();
        authenticatedUser = users.getFirst();
        user2 = users.get(1);
        user3 = users.get(2);
        FollowHelpers.seedFollow(followRepository, authenticatedUser, user2);
        FollowHelpers.seedFollow(followRepository, authenticatedUser, user3);
      }

      @Test
      void followingNoCursor_returnsUserConnection() {
        // Utilises the default first value (first 5 results)
        authenticatedTester()
            .document(
                """
                    {
                       me {
                         id
                         following {
                           edges {
                              node {
                                id
                             }
                           }
                         }
                         followingCount
                       }
                     }""")
            .execute()
            .path("me")
            .matchesJson(
                String.format(
                    """
                        {
                          "id": "%s",
                          "following": {
                            "edges": [
                            {
                              "node": {
                                "id": "%s"
                               }
                            },
                            {
                              "node": {
                                "id": "%s"
                               }
                            }
                            ]
                          },
                          "followingCount": 2
                        }
                        """,
                    authenticatedUser.getId(), user2.getId(), user3.getId()));
      }

      @Test
      void paginationWithValidAfter_returnsNextUserConnection() {
        UserConnection firstPage =
            authenticatedTester()
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
        assertThat(firstPage.edges().getFirst().node().id()).isEqualTo(user3.getId());
        assertThat(firstPage.edges().getFirst().cursor()).isEqualTo(firstPageEndCursor);
        assertTrue(firstPage.pageInfo().hasNextPage());

        UserConnection secondPage =
            authenticatedTester()
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

        assertThat(secondPage.edges().getFirst().node().id()).isEqualTo(user2.getId());
        assertThat(secondPage.edges().getFirst().cursor())
            .isEqualTo(secondPage.pageInfo().endCursor());
        assertFalse(secondPage.pageInfo().hasNextPage());
      }

      @Test
      void paginationWithMalformedAfter_returnsProtocolError() {
        String malformedAfter = "not-base64!";
        // Act
        authenticatedTester()
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

        // Act
        UserConnection emptyData =
            authenticatedTester()
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
                .isEqualTo(2L)
                .path("me.following")
                .entity(UserConnection.class)
                .get();

        // Assert
        assertThat(emptyData.edges()).hasSize(0);
        assertFalse(emptyData.pageInfo().hasNextPage());
      }
    }

    @Nested
    class followersTests {
      /**
       * Following system:
       *
       * <p>user 1 is followed by:
       *
       * <ul>
       *   <li>user 2
       *   <li>user 3
       * </ul>
       */
      @BeforeEach
      void setup() {
        authenticatedUser = users.getFirst();
        user2 = users.get(1);
        user3 = users.get(2);
        FollowHelpers.seedFollow(followRepository, user2, authenticatedUser);
        FollowHelpers.seedFollow(followRepository, user3, authenticatedUser);
      }

      @Test
      void followersNoCursor_returnsUserConnection() {
        authenticatedTester()
            .document(
                """
                    {
                       me {
                         id
                         followers {
                           edges {
                              node {
                                id
                             }
                           }
                         }
                         followerCount
                       }
                     }""")
            .execute()
            .path("me")
            .matchesJson(
                String.format(
                    """
                        {
                          "id": "%s",
                          "followers": {
                            "edges": [
                            {
                              "node": {
                                "id": "%s"
                               }
                            },
                            {
                              "node": {
                                "id": "%s"
                               }
                            }
                            ]
                          },
                          "followerCount": 2
                        }""",
                    authenticatedUser.getId(),
                    // user3 is a more recent follower -> displayed first
                    user3.getId(),
                    user2.getId()));
      }

      @Test
      void paginationWithValidAfter_returnsNextUserConnection() {
        UserConnection firstPage =
            authenticatedTester()
                .document(
                    """
                        {
                          me {
                            followers(first: 1) {
                              edges {
                                node {
                                  id
                                }
                                cursor
                              }
                              pageInfo {
                                hasNextPage
                                endCursor
                              }
                            }
                            followerCount
                          }
                        }
                        """)
                .execute()
                .path("me.followerCount")
                .entity(Long.class)
                .isEqualTo(2L)
                .path("me.followers")
                .entity(UserConnection.class)
                .get();

        String firstPageEndCursor = firstPage.pageInfo().endCursor();

        // Followings are sorted by most recent -> second follow will be the first page
        assertThat(firstPage.edges().getFirst().node().id()).isEqualTo(user3.getId());
        assertThat(firstPage.edges().getFirst().cursor()).isEqualTo(firstPageEndCursor);
        assertTrue(firstPage.pageInfo().hasNextPage());

        UserConnection secondPage =
            authenticatedTester()
                .document(
                    """
                        query getFollowers($after: String) {
                          me {
                            followers(first: 1, after: $after) {
                              edges {
                                node {
                                  id
                                }
                                cursor
                              }
                              pageInfo {
                                hasNextPage
                                endCursor
                              }
                            }
                            followerCount
                          }
                        }
                        """)
                .variable("after", firstPageEndCursor)
                .execute()
                .path("me.followerCount")
                .entity(Long.class)
                .isEqualTo(2L)
                .path("me.followers")
                .entity(UserConnection.class)
                .get();

        assertThat(secondPage.edges().getFirst().node().id()).isEqualTo(user2.getId());
        assertThat(secondPage.edges().getFirst().cursor())
            .isEqualTo(secondPage.pageInfo().endCursor());
        assertFalse(secondPage.pageInfo().hasNextPage());
      }

      @Test
      void paginationWithMalformedAfter_returnsProtocolError() {
        String malformedAfter = "not-base64!";
        // Act
        authenticatedTester()
            .document(
                """
                    query getFollowers($after: String) {
                      me {
                        followers(first: 1, after: $after) {
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

        // Act
        UserConnection emptyData =
            authenticatedTester()
                .document(
                    """
                        query getFollowers($after: String) {
                          me {
                            followers(first: 1, after: $after) {
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
                            followerCount
                          }
                        }
                        """)
                .variable("after", cursorPastEnd)
                .execute()
                .path("me.followerCount")
                .entity(Long.class)
                .isEqualTo(2L)
                .path("me.followers")
                .entity(UserConnection.class)
                .get();

        // Assert
        assertThat(emptyData.edges()).hasSize(0);
        assertFalse(emptyData.pageInfo().hasNextPage());
      }
    }
  }

  @Nested
  class batchMappingTests {
    @Autowired FollowRepository followRepository;

    @AfterEach
    void cleanup() {
      followRepository.deleteAll();
    }

    @Test
    void getUserWithFollowersAndIsFollowing() {
      // Initialise
      User authenticatedUser = users.getFirst();
      User user2 = users.get(1);
      User user3 = users.get(2);
      // both user 1 and user 2 follow authenticatedUser
      FollowHelpers.seedFollow(followRepository, user2, authenticatedUser);
      FollowHelpers.seedFollow(followRepository, user3, authenticatedUser);
      // authenticated user only follows user 3
      FollowHelpers.seedFollow(followRepository, authenticatedUser, user3);

      authenticatedTester()
          .document(
              String.format(
                  """
                      {
                         me {
                           id
                           followers(first: %d) {
                             edges {
                                node {
                                  id
                                  isFollowing
                               }
                             }
                           }
                           followerCount
                         }
                       }""",
                  5))
          .execute()
          .path("me")
          .matchesJson(
              String.format(
                  """
                      {
                        "id": "%s",
                        "followers": {
                          "edges": [
                          {
                            "node": {
                              "id": "%s",
                              "isFollowing": true
                             }
                          },
                          {
                            "node": {
                              "id": "%s",
                              "isFollowing": false
                             }
                          }
                          ]
                        },
                        "followerCount": 2
                      }""",
                  authenticatedUser.getId(), user3.getId(), user2.getId()));
    }
  }
}
