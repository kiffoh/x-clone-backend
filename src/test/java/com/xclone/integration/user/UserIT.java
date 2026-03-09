package com.xclone.integration.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import com.xclone.integration.base.BaseIntegrationTest;
import com.xclone.support.fixtures.UserFixtures;
import com.xclone.support.helpers.AuthHelpers;
import com.xclone.user.model.entity.User;
import com.xclone.user.repository.UserRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
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

  // Can I initialise here or is in the before all correct?
  static List<String> handles;

  List<User> users;

  String accessToken;

  @BeforeAll
  static void initialisation() {
    handles = List.of("example1", "example2", "example3");
  }

  @BeforeEach
  void setup() {
    userRepository.deleteAll();
    users =
        handles.stream().map(UserFixtures::createUserWithHandle).map(userRepository::save).toList();
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
    @Test
    void userById_userExists_returnsUser() {
      User user = users.getFirst();
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
                  user.getId()))
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
  class searchUsersTest {
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
}
