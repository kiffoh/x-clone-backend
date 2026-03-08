package com.xclone.integration.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.xclone.exception.dto.ErrorResponse;
import com.xclone.integration.base.BaseIntegrationTest;
import com.xclone.support.fixtures.UserFixtures;
import com.xclone.support.helpers.AuthHelpers;
import com.xclone.user.model.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * Authenticated requests are implicitly tested in any IT which requires an access token.
 * Consequently, they are not tested here to remove duplication.
 */
@Import(AuthHelpers.class)
public class SecurityIT extends BaseIntegrationTest {

  @Autowired
  private TestRestTemplate testRestTemplate;

  @Autowired
  AuthHelpers authHelpers;

  HttpHeaders headers;

  @BeforeEach
  void setup() {
    headers = new HttpHeaders();
  }

  @Test
  void unauthenticatedRequest_noAccessToken_returns401() {
    ResponseEntity<ErrorResponse> response =
        testRestTemplate.postForEntity("/graphql", new HttpEntity<>(headers), ErrorResponse.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
  }

  @Test
  void unauthenticatedRequest_invalidAccessToken_returns401() {
    headers.setBearerAuth("notValidAccessToken");

    ResponseEntity<ErrorResponse> response =
        testRestTemplate.postForEntity("/graphql", new HttpEntity<>(headers), ErrorResponse.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
  }

  @Test
  void unauthenticatedRequest_expiredAccessToken_returns401() {
    User user = UserFixtures.getDefaultUserWithRandomId();
    headers.setBearerAuth(
        authHelpers.createExpiredAccessToken(
            user.getId().toString(), user.getRole().toString()));

    ResponseEntity<ErrorResponse> response =
        testRestTemplate.postForEntity("/graphql", new HttpEntity<>(headers), ErrorResponse.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
  }
}
