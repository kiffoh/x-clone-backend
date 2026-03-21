package com.xclone.user.contoller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.xclone.config.GraphQlConfig;
import com.xclone.exception.custom.DuplicateHandleException;
import com.xclone.support.fixtures.UserFixtures;
import com.xclone.user.controller.UserController;
import com.xclone.user.dto.connection.UserConnection;
import com.xclone.user.model.entity.User;
import com.xclone.user.service.UserService;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.graphql.GraphQlTest;
import org.springframework.context.annotation.Import;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@GraphQlTest(UserController.class)
@Import(GraphQlConfig.class)
public class UserControllerTest {

  @MockitoBean private UserService userService;

  @Autowired GraphQlTester tester;

  @Test
  @WithMockCustomUser
  public void me_returnsUserProfile() {
    User defaultUser = UserFixtures.getDefaultUserWithStaticId();
    String request =
        """
            {
              me {
                handle
                id
              }
            }
            """;
    tester
        .document(request)
        .execute()
        .path("me.handle")
        .entity(String.class)
        .isEqualTo(defaultUser.getHandle())
        .path("me.id")
        .entity(String.class)
        .isEqualTo(defaultUser.getId().toString());
  }

  @Test
  public void userByHandle_returnsUserProfile() {
    String handle = "exampleHandle";
    User defaultUser = UserFixtures.getDefaultUserWithRandomId();
    when(userService.getUserByHandle(handle)).thenReturn(defaultUser.toUserProfile());
    String request =
        String.format(
            """
                {
                  userByHandle(handle: "%s") {
                    id
                  }
                }
                """,
            handle);
    tester
        .document(request)
        .execute()
        .path("userByHandle.id")
        .entity(String.class)
        .isEqualTo(defaultUser.getId().toString());
  }

  @Test
  public void userById_returnsUserProfile() {
    User defaultUser = UserFixtures.getDefaultUserWithRandomId();
    UUID id = defaultUser.getId();
    when(userService.getUserById(id)).thenReturn(defaultUser.toUserProfile());
    String request =
        String.format(
            """
                {
                  userById(id: "%s") {
                    id
                  }
                }
                """,
            id);
    tester
        .document(request)
        .execute()
        .path("userById.id")
        .entity(String.class)
        .isEqualTo(id.toString());
  }

  @Test
  public void searchUsers_returnsUserProfile() {
    String query = "exam";
    UserConnection userConnection = UserFixtures.getDefaultUserConnection();
    when(userService.getUsersByHandle(query)).thenReturn(userConnection);
    String request =
        String.format(
            """
                {
                  searchUsers(query: "%s") {
                    totalCount
                    edges {
                      node {
                        id
                        handle
                      }
                     }
                   }
                }
                """,
            query);
    tester
        .document(request)
        .execute()
        .path("searchUsers.totalCount")
        .entity(Integer.class)
        .isEqualTo(3);
  }

  @Test
  @WithMockCustomUser
  public void updateMyProfile_returnsUserResponse() {
    User defaultUser = UserFixtures.getDefaultUserWithStaticId();
    String newDisplayName = "newName";
    String newHandle = "newHandle";
    User updatedUser = defaultUser.toBuilder().build();
    updatedUser.setDisplayName(newDisplayName);
    updatedUser.setHandle(newHandle);
    when(userService.updateProfile(anyString(), any())).thenReturn(updatedUser.toUserProfile());

    tester
        .document(
            """
            mutation UpdateProfile($input: UpdateUserInput!) {
              updateMyProfile(input: $input) {
                code
                success
                user {
                  displayName
                  handle
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
                "handle", newHandle))
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
                "handle": "%s"
              },
              "errors": null
            }
            """,
                newDisplayName, newHandle));
  }

  @Test
  @WithMockCustomUser
  public void updateMyProfile_returnsDuplicateHandle() {
    User defaultUser = UserFixtures.getDefaultUserWithStaticId();
    when(userService.updateProfile(anyString(), any()))
        .thenThrow(new DuplicateHandleException("Handle already in use"));

    tester
        .document(
            """
            mutation UpdateProfile($input: UpdateUserInput!) {
              updateMyProfile(input: $input) {
                code
                success
                user {
                  displayName
                  handle
                }
                errors {
                  field
                  message
                }
              }
            }
            """)
        .variable("input", Map.of("handle", defaultUser.getHandle()))
        .execute()
        .path("updateMyProfile")
        .matchesJson(
            """
            {
              "code": "409",
              "success": false,
              "user": null,
              "errors": [
                { "field": "handle", "message" : "Handle already in use" }
              ]
            }
            """);
  }

  @Test
  @WithMockCustomUser
  public void deleteMyAccount_returnsDeleteResponse() {
    tester
        .document(
            """
            mutation DeleteProfile {
              deleteMyAccount {
                code
                success
                errors {
                  field
                  message
                }
              }
            }
            """)
        .execute()
        .path("deleteMyAccount")
        .matchesJson(
            """
            {
              "code": "200",
              "success": true,
              "errors": null
            }
            """);
  }
}
