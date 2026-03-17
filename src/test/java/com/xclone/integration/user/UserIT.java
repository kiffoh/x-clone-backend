package com.xclone.integration.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.xclone.common.mutation.DeleteResponse;
import com.xclone.exception.dto.FieldError;
import com.xclone.integration.base.BaseIntegrationTest;
import com.xclone.support.fixtures.UserFixtures;
import com.xclone.support.helpers.AuthHelpers;
import com.xclone.user.dto.mutation.UserResponse;
import com.xclone.user.model.entity.User;
import com.xclone.user.model.enums.UserStatus;
import com.xclone.user.repository.UserRepository;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

  List<String> handles = List.of("example1", "example2", "example3");

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
              tuple("handle", "size must be between 4 and 15"),
              tuple("handle", "must match \"^(?![0-9]+$)[0-9a-zA-Z_]+$\""));
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
                          totalCount
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
          .path("searchUsers.totalCount")
          .entity(Integer.class)
          .isEqualTo(3)
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
                          totalCount
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
          .path("searchUsers.totalCount")
          .entity(Integer.class)
          .isEqualTo(1)
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
                          totalCount
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
          .path("searchUsers.totalCount")
          .entity(Integer.class)
          .isEqualTo(0)
          .path("searchUsers.edges[*].node.handle")
          .entityList(String.class)
          .hasSize(0);
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
              tuple("handle", "size must be between 4 and 15"),
              tuple("handle", "must match \"^(?![0-9]+$)[0-9a-zA-Z_]+$\""));
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
}
