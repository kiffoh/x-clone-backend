package com.xclone.integration.validation;

import static com.xclone.support.helpers.PostHelpers.seedPosts;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.xclone.auth.dto.SignupRequest;
import com.xclone.exception.dto.FieldError;
import com.xclone.exception.dto.ValidationErrorResponse;
import com.xclone.integration.base.BaseAuthIntegrationTest;
import com.xclone.post.dto.mutation.PostResponse;
import com.xclone.post.model.entity.Post;
import com.xclone.post.repository.PostRepository;
import com.xclone.support.fixtures.UserFixtures;
import com.xclone.support.helpers.AuthHelpers;
import com.xclone.user.dto.mutation.UserResponse;
import com.xclone.user.model.entity.User;
import com.xclone.user.repository.UserRepository;
import com.xclone.validation.ValidationConstants;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.graphql.tester.AutoConfigureHttpGraphQlTester;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.graphql.test.tester.HttpGraphQlTester;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@AutoConfigureHttpGraphQlTester
@Import(AuthHelpers.class)
public class ValidationIT extends BaseAuthIntegrationTest {
  @Autowired UserRepository userRepository;
  @Autowired PostRepository postRepository;
  @Autowired AuthHelpers authHelpers;
  @Autowired HttpGraphQlTester graphQlTester;
  @Autowired TestRestTemplate testRestTemplate;

  @BeforeEach
  void cleanupDBs() {
    postRepository.deleteAll();
    userRepository.deleteAll();
  }

  @Nested
  class ValidHandleTests {
    @Test
    void invalidHandle_invalidCharacter_returns400() {
      SignupRequest request = new SignupRequest("handle?", "Validpassword1!", null, null, null);

      ResponseEntity<ValidationErrorResponse> response =
          testRestTemplate.postForEntity(
              "/api/auth/signup", request, ValidationErrorResponse.class);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
      assertThat(response.getBody().errors())
          .isEqualTo(List.of(new FieldError("handle", ValidationConstants.INVALID_HANDLE_REGEX)));
    }

    @Test
    void invalidHandle_allNumbers_returns400() {
      SignupRequest request = new SignupRequest("0000", "Validpassword1!", null, null, null);

      ResponseEntity<ValidationErrorResponse> response =
          testRestTemplate.postForEntity(
              "/api/auth/signup", request, ValidationErrorResponse.class);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
      assertThat(response.getBody().errors())
          .isEqualTo(List.of(new FieldError("handle", ValidationConstants.INVALID_HANDLE_REGEX)));
    }

    @Test
    void invalidHandle_tooShort_returns400() {
      SignupRequest request = new SignupRequest("ex", "Validpassword1!", null, null, null);

      ResponseEntity<ValidationErrorResponse> response =
          testRestTemplate.postForEntity(
              "/api/auth/signup", request, ValidationErrorResponse.class);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
      assertThat(response.getBody().errors())
          .isEqualTo(List.of(new FieldError("handle", ValidationConstants.INVALID_HANDLE_SIZE)));
    }

    @Test
    void invalidHandle_tooLong_returns400() {
      SignupRequest request =
          new SignupRequest("handleWhichIsTooLong", "Validpassword1!", null, null, null);

      ResponseEntity<ValidationErrorResponse> response =
          testRestTemplate.postForEntity(
              "/api/auth/signup", request, ValidationErrorResponse.class);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
      assertThat(response.getBody().errors())
          .isEqualTo(List.of(new FieldError("handle", ValidationConstants.INVALID_HANDLE_SIZE)));
    }
  }

  @Nested
  class ValidPasswordTests {
    @Test
    void invalidPassword_invalidCharacter_returns400() {
      SignupRequest request = new SignupRequest("handle", "Invalidpassword1!~", null, null, null);

      ResponseEntity<ValidationErrorResponse> response =
          testRestTemplate.postForEntity(
              "/api/auth/signup", request, ValidationErrorResponse.class);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
      assertThat(response.getBody().errors())
          .isEqualTo(
              List.of(new FieldError("password", ValidationConstants.INVALID_PASSWORD_REGEX)));
    }

    @Test
    void invalidPassword_noCapitalLetter_returns400() {
      SignupRequest request = new SignupRequest("handle", "invalidpassword1!", null, null, null);

      ResponseEntity<ValidationErrorResponse> response =
          testRestTemplate.postForEntity(
              "/api/auth/signup", request, ValidationErrorResponse.class);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
      assertThat(response.getBody().errors())
          .isEqualTo(
              List.of(new FieldError("password", ValidationConstants.INVALID_PASSWORD_REGEX)));
    }

    @Test
    void invalidPassword_noLowercaseLetter_returns400() {
      SignupRequest request = new SignupRequest("handle", "INVALIDPASSWORD1!", null, null, null);

      ResponseEntity<ValidationErrorResponse> response =
          testRestTemplate.postForEntity(
              "/api/auth/signup", request, ValidationErrorResponse.class);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
      assertThat(response.getBody().errors())
          .isEqualTo(
              List.of(new FieldError("password", ValidationConstants.INVALID_PASSWORD_REGEX)));
    }

    @Test
    void invalidPassword_noNumber_returns400() {
      SignupRequest request = new SignupRequest("handle", "Invalidpassword!", null, null, null);

      ResponseEntity<ValidationErrorResponse> response =
          testRestTemplate.postForEntity(
              "/api/auth/signup", request, ValidationErrorResponse.class);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
      assertThat(response.getBody().errors())
          .isEqualTo(
              List.of(new FieldError("password", ValidationConstants.INVALID_PASSWORD_REGEX)));
    }

    @Test
    void invalidPassword_noSpecialCharacter_returns400() {
      SignupRequest request = new SignupRequest("handle", "Invalidpassword1", null, null, null);

      ResponseEntity<ValidationErrorResponse> response =
          testRestTemplate.postForEntity(
              "/api/auth/signup", request, ValidationErrorResponse.class);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
      assertThat(response.getBody().errors())
          .isEqualTo(
              List.of(new FieldError("password", ValidationConstants.INVALID_PASSWORD_REGEX)));
    }

    @Test
    void invalidPassword_tooShort_returns400() {
      SignupRequest request = new SignupRequest("handle", "Invalid1!", null, null, null);

      ResponseEntity<ValidationErrorResponse> response =
          testRestTemplate.postForEntity(
              "/api/auth/signup", request, ValidationErrorResponse.class);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
      assertThat(response.getBody().errors())
          .isEqualTo(
              List.of(new FieldError("password", ValidationConstants.INVALID_PASSWORD_SIZE)));
    }

    @Test
    void invalidPassword_tooLong_returns400() {
      String longPassword = "Invalidpassword1!".repeat(10);
      SignupRequest request = new SignupRequest("handle", longPassword, null, null, null);

      ResponseEntity<ValidationErrorResponse> response =
          testRestTemplate.postForEntity(
              "/api/auth/signup", request, ValidationErrorResponse.class);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
      assertThat(response.getBody().errors())
          .isEqualTo(
              List.of(new FieldError("password", ValidationConstants.INVALID_PASSWORD_SIZE)));
    }
  }

  @Nested
  class ObjectNotEmptyTests {
    User authenticatedUser;
    String accessToken;

    @BeforeEach
    void setup() {
      userRepository.deleteAll();
      authenticatedUser = userRepository.save(UserFixtures.createUserWithHandle("example1"));
      accessToken = authHelpers.getUserAccessToken(authenticatedUser.getId().toString());
    }

    // Helpers
    private HttpGraphQlTester authenticatedTester() {
      return graphQlTester.mutate().headers(headers -> headers.setBearerAuth(accessToken)).build();
    }

    @Test
    void allNullInput_returnsInvalidRequest() {
      Map<String, Object> input = new HashMap<>();
      input.put("displayName", null);
      input.put("handle", null);
      input.put("bio", null);
      input.put("profileImage", null);

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
          .containsExactly(
              tuple("updateUserInput", "UpdateUserInput must have at least one field"));
    }
  }

  @Nested
  public class malformedCursorTests {
    User authenticatedUser;
    String accessToken;

    @BeforeEach
    void setup() {
      userRepository.deleteAll();
      authenticatedUser = userRepository.save(UserFixtures.createUserWithHandle("example1"));
      accessToken = authHelpers.getUserAccessToken(authenticatedUser.getId().toString());
    }

    // Helpers
    private HttpGraphQlTester authenticatedTester() {
      return graphQlTester.mutate().headers(headers -> headers.setBearerAuth(accessToken)).build();
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
  }

  @Nested
  class CreatePostInputTests {
    User authenticatedUser;
    String accessToken;
    HttpGraphQlTester authenticatedTester;

    @BeforeEach
    void setup() {
      userRepository.deleteAll();
      authenticatedUser = userRepository.save(UserFixtures.createUserWithHandle("example1"));
      accessToken = authHelpers.getUserAccessToken(authenticatedUser.getId().toString());
      authenticatedTester =
          graphQlTester.mutate().headers(headers -> headers.setBearerAuth(accessToken)).build();
    }

    @Test
    void invalidMessageContent_tooLong_returns400() {
      String longMessage = "this message will be over 280 characters".repeat(20);
      Map<String, Object> createPostInput = new HashMap<>();
      createPostInput.put("messageContent", longMessage);

      PostResponse response =
          authenticatedTester
              .document(
                  """
                      mutation CreatePost($input: CreatePostInput!) {
                        createPost(input: $input) {
                          code
                          success
                          post {
                            messageContent
                            author {
                              id
                            }
                          }
                          errors {
                            field
                            message
                          }
                        }
                      }
                      """)
              .variable("input", createPostInput)
              .execute()
              .path("createPost")
              .entity(PostResponse.class)
              .get();

      assertEquals("400", response.code());
      assertFalse(response.success());
      assertNull(response.post());
      assertThat(response.errors())
          .extracting(FieldError::field, FieldError::message)
          .containsExactly(
              tuple("messageContent", ValidationConstants.INVALID_MESSAGE_CONTENT_SIZE));
    }

    @Test
    void invalidMessageContent_empty_returns400() {
      Map<String, Object> createPostInput = new HashMap<>();
      createPostInput.put("messageContent", "");

      PostResponse response =
          authenticatedTester
              .document(
                  """
                      mutation CreatePost($input: CreatePostInput!) {
                        createPost(input: $input) {
                          code
                          success
                          post {
                            messageContent
                            author {
                              id
                            }
                          }
                          errors {
                            field
                            message
                          }
                        }
                      }
                      """)
              .variable("input", createPostInput)
              .execute()
              .path("createPost")
              .entity(PostResponse.class)
              .get();

      assertEquals("400", response.code());
      assertFalse(response.success());
      assertNull(response.post());
      assertThat(response.errors())
          .extracting(FieldError::field, FieldError::message)
          .containsExactly(tuple("messageContent", "Post message content is required"));
    }

    @Test
    void nullMessageContent_returns400() {
      Map<String, Object> createPostInput = new HashMap<>();
      createPostInput.put("messageContent", null);

      authenticatedTester
          .document(
              """
                  mutation CreatePost($input: CreatePostInput!) {
                    createPost(input: $input) {
                      code
                      success
                      post {
                        messageContent
                        author {
                          id
                        }
                      }
                      errors {
                        field
                        message
                      }
                    }
                  }
                  """)
          .variable("input", createPostInput)
          .execute()
          .errors()
          .filter(error -> "BAD_REQUEST".equals(error.getExtensions().get("classification")))
          .expect(error -> error.getMessage().contains("Field 'messageContent' has coerced Null"));
    }
  }

  @Nested
  class UpdatePostInputTests {
    User authenticatedUser;
    String accessToken;
    HttpGraphQlTester authenticatedTester;

    @BeforeEach
    void setup() {
      userRepository.deleteAll();
      authenticatedUser = userRepository.save(UserFixtures.createUserWithHandle("example1"));
      accessToken = authHelpers.getUserAccessToken(authenticatedUser.getId().toString());
      authenticatedTester =
          graphQlTester.mutate().headers(headers -> headers.setBearerAuth(accessToken)).build();
    }

    @Test
    void invalidMessageContent_tooLong_returns400() {
      String longMessage = "this message will be over 280 characters".repeat(20);
      Map<String, Object> updatePostInput = new HashMap<>();
      updatePostInput.put("messageContent", longMessage);
      updatePostInput.put("postId", UUID.randomUUID());

      PostResponse response =
          authenticatedTester
              .document(
                  """
                      mutation UpdatePost($input: UpdatePostInput!) {
                        updatePostContent(input: $input) {
                          code
                          success
                          post {
                            messageContent
                          }
                          errors {
                            field
                            message
                          }
                        }
                      }
                      """)
              .variable("input", updatePostInput)
              .execute()
              .path("updatePostContent")
              .entity(PostResponse.class)
              .get();

      assertEquals("400", response.code());
      assertFalse(response.success());
      assertNull(response.post());
      assertThat(response.errors())
          .extracting(FieldError::field, FieldError::message)
          .containsExactly(
              tuple("messageContent", ValidationConstants.INVALID_MESSAGE_CONTENT_SIZE));
    }

    @Test
    void invalidMessageContent_empty_returns400() {
      Map<String, Object> updatePostInput = new HashMap<>();
      updatePostInput.put("messageContent", "");
      updatePostInput.put("postId", UUID.randomUUID());

      PostResponse response =
          authenticatedTester
              .document(
                  """
                      mutation UpdatePost($input: UpdatePostInput!) {
                        updatePostContent(input: $input) {
                          code
                          success
                          post {
                            messageContent
                          }
                          errors {
                            field
                            message
                          }
                        }
                      }
                      """)
              .variable("input", updatePostInput)
              .execute()
              .path("updatePostContent")
              .entity(PostResponse.class)
              .get();

      assertEquals("400", response.code());
      assertFalse(response.success());
      assertNull(response.post());
      assertThat(response.errors())
          .extracting(FieldError::field, FieldError::message)
          .containsExactly(tuple("messageContent", "Post message content is required"));
    }

    @Test
    void nullMessageContent_returns400() {
      Map<String, Object> updatePostInput = new HashMap<>();
      updatePostInput.put("messageContent", null);
      updatePostInput.put("postId", UUID.randomUUID());

      authenticatedTester
          .document(
              """
                    mutation UpdatePost($input: UpdatePostInput!) {
                        updatePostContent(input: $input) {
                          code
                          success
                          post {
                            messageContent
                          }
                          errors {
                            field
                            message
                          }
                        }
                      }
                  """)
          .variable("input", updatePostInput)
          .execute()
          .errors()
          .filter(error -> "BAD_REQUEST".equals(error.getExtensions().get("classification")))
          .expect(error -> error.getMessage().contains("Field 'messageContent' has coerced Null"));
    }

    @Test
    void nullParentId_returns400() {
      Map<String, Object> updatePostInput = new HashMap<>();
      updatePostInput.put("messageContent", "valid message");
      updatePostInput.put("postId", null);

      authenticatedTester
          .document(
              """
                    mutation UpdatePost($input: UpdatePostInput!) {
                        updatePostContent(input: $input) {
                          code
                          success
                          post {
                            messageContent
                          }
                          errors {
                            field
                            message
                          }
                        }
                      }
                  """)
          .variable("input", updatePostInput)
          .execute()
          .errors()
          .filter(error -> "BAD_REQUEST".equals(error.getExtensions().get("classification")))
          .expect(error -> error.getMessage().contains("Field 'postId' has coerced Null"));
    }
  }

  @Nested
  class CreateReplyInputTests {
    User authenticatedUser;
    String accessToken;
    HttpGraphQlTester authenticatedTester;
    List<Post> posts;

    @BeforeEach
    void setup() {
      userRepository.deleteAll();
      authenticatedUser = userRepository.save(UserFixtures.createUserWithHandle("example1"));
      accessToken = authHelpers.getUserAccessToken(authenticatedUser.getId().toString());
      authenticatedTester =
          graphQlTester.mutate().headers(headers -> headers.setBearerAuth(accessToken)).build();
      posts = seedPosts(List.of("original post"), List.of(authenticatedUser), postRepository);
    }

    @Test
    void invalidMessageContent_tooLong_returns400() {
      String longMessage = "this message will be over 280 characters".repeat(20);
      Map<String, Object> createReplyInput = new HashMap<>();
      createReplyInput.put("messageContent", longMessage);
      createReplyInput.put("parentId", posts.getFirst().getId().toString());

      PostResponse response =
          authenticatedTester
              .document(
                  """
                      mutation CreateReply($input: CreateReplyInput!) {
                        createReply(input: $input) {
                          code
                          success
                          post {
                            messageContent
                            author {
                              id
                            }
                            parent {
                              id
                            }
                          }
                          errors {
                            field
                            message
                          }
                        }
                      }
                      """)
              .variable("input", createReplyInput)
              .execute()
              .path("createReply")
              .entity(PostResponse.class)
              .get();

      assertEquals("400", response.code());
      assertFalse(response.success());
      assertNull(response.post());
      assertThat(response.errors())
          .extracting(FieldError::field, FieldError::message)
          .containsExactly(
              tuple("messageContent", ValidationConstants.INVALID_MESSAGE_CONTENT_SIZE));
    }

    @Test
    void invalidMessageContent_empty_returns400() {
      Map<String, Object> createReplyInput = new HashMap<>();
      createReplyInput.put("messageContent", "");
      createReplyInput.put("parentId", posts.getFirst().getId().toString());

      PostResponse response =
          authenticatedTester
              .document(
                  """
                      mutation CreateReply($input: CreateReplyInput!) {
                        createReply(input: $input) {
                          code
                          success
                          post {
                            messageContent
                            author {
                              id
                            }
                            parent {
                              id
                            }
                          }
                          errors {
                            field
                            message
                          }
                        }
                      }
                      """)
              .variable("input", createReplyInput)
              .execute()
              .path("createReply")
              .entity(PostResponse.class)
              .get();

      assertEquals("400", response.code());
      assertFalse(response.success());
      assertNull(response.post());
      assertThat(response.errors())
          .extracting(FieldError::field, FieldError::message)
          .containsExactly(tuple("messageContent", "Post message content is required"));
    }

    @Test
    void nullMessageContent_returns400() {
      Map<String, Object> createReplyInput = new HashMap<>();
      createReplyInput.put("messageContent", null);
      createReplyInput.put("parentId", posts.getFirst().getId().toString());

      authenticatedTester
          .document(
              """
                  mutation CreateReply($input: CreateReplyInput!) {
                    createReply(input: $input) {
                      code
                      success
                      post {
                        messageContent
                        author {
                          id
                        }
                        parent {
                          id
                        }
                      }
                      errors {
                        field
                        message
                      }
                    }
                  }
                  """)
          .variable("input", createReplyInput)
          .execute()
          .errors()
          .filter(error -> "BAD_REQUEST".equals(error.getExtensions().get("classification")))
          .expect(error -> error.getMessage().contains("Field 'messageContent' has coerced Null"));
    }

    @Test
    void nullParentId_returns400() {
      Map<String, Object> createReplyInput = new HashMap<>();
      createReplyInput.put("messageContent", "original message");
      createReplyInput.put("parentId", null);

      authenticatedTester
          .document(
              """
                  mutation CreateReply($input: CreateReplyInput!) {
                    createReply(input: $input) {
                      code
                      success
                      post {
                        messageContent
                        author {
                          id
                        }
                        parent {
                          id
                        }
                      }
                      errors {
                        field
                        message
                      }
                    }
                  }
                  """)
          .variable("input", createReplyInput)
          .execute()
          .errors()
          .filter(error -> "BAD_REQUEST".equals(error.getExtensions().get("classification")))
          .expect(error -> error.getMessage().contains("Field 'parentId' has coerced Null"));
    }
  }
}
