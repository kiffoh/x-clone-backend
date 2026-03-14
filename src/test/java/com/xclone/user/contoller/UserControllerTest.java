package com.xclone.user.contoller;

import static org.mockito.Mockito.when;

import com.xclone.config.GraphQlConfig;
import com.xclone.support.fixtures.UserFixtures;
import com.xclone.user.controller.UserController;
import com.xclone.user.dto.connection.UserConnection;
import com.xclone.user.model.entity.User;
import com.xclone.user.service.UserService;
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
}
