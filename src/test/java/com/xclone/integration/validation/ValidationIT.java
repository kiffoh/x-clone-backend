package com.xclone.integration.validation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.xclone.auth.dto.SignupRequest;
import com.xclone.exception.dto.FieldError;
import com.xclone.exception.dto.ValidationErrorResponse;
import com.xclone.integration.base.BaseAuthIntegrationTest;
import com.xclone.support.fixtures.UserFixtures;
import com.xclone.support.helpers.AuthHelpers;
import com.xclone.user.dto.mutation.UserResponse;
import com.xclone.user.model.entity.User;
import com.xclone.user.repository.UserRepository;
import com.xclone.validation.ValidationConstants;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
  @Autowired AuthHelpers authHelpers;
  @Autowired HttpGraphQlTester graphQlTester;
  @Autowired TestRestTemplate testRestTemplate;

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
}
