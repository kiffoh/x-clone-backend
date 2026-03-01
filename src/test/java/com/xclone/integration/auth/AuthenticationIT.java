package com.xclone.integration.auth;


import static org.assertj.core.api.Assertions.assertThat;

import com.xclone.auth.dto.AuthResponse;
import com.xclone.auth.dto.LoginRequest;
import com.xclone.auth.dto.SignupRequest;
import com.xclone.exception.dto.ErrorResponse;
import com.xclone.exception.dto.ValidationErrorResponse;
import com.xclone.integration.base.BaseAuthIntegrationTest;
import com.xclone.user.model.entity.User;
import com.xclone.user.model.enums.UserStatus;
import com.xclone.user.repository.UserRepository;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public class AuthenticationIT extends BaseAuthIntegrationTest {

  @Autowired
  private TestRestTemplate testRestTemplate;
  @Autowired
  private UserRepository userRepository;
  @Autowired
  private StringRedisTemplate stringRedisTemplate;

  private final String refreshTokenRegex =
      "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}";

  @BeforeEach
  void setup() {
    userRepository.deleteAll();
    stringRedisTemplate.execute((RedisCallback<Void>) connection -> {
      connection.serverCommands().flushDb();
      return null;
    });
  }

  // Helpers
  private ResponseEntity<AuthResponse> signup() {
    return testRestTemplate.postForEntity(
        "/api/auth/signup",
        new SignupRequest("exampleHandle", "password", null, null, null),
        AuthResponse.class);
  }

  private String extractRefreshCookie(ResponseEntity<?> response) {
    return response.getHeaders().get("Set-Cookie")
        .getFirst().split(";")[0];
  }

  @Test
  void signup_validRequest_returns200AndSetsRefreshCookie() {
    SignupRequest request = new SignupRequest("exampleHandle", "password", null, null, null);

    ResponseEntity<AuthResponse> response =
        testRestTemplate.postForEntity("/api/auth/signup", request, AuthResponse.class);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getHeaders().get("Set-Cookie"))
        .anyMatch(cookie -> cookie.matches("refreshToken=" + refreshTokenRegex + ";.*"));
    Assertions.assertNotNull(response.getBody());
    assertThat(response.getBody().accessToken()).isNotNull();
  }

  @Test
  void signup_validRequest_persistsUserToDatabase() {
    signup();

    // User persisted
    List<User> users = userRepository.findAll();
    assertThat(users).hasSize(1);
    assertThat(users.getFirst().getHandle()).isEqualTo("exampleHandle");
  }

  @Test
  void signup_validRequest_storesRefreshTokenInRedis() {
    signup();

    // Refresh token stored in redis
    Set<String> keys = stringRedisTemplate.keys("refresh_token:*");
    assertThat(keys).hasSize(1);
  }

  @Test
  void signup_duplicateHandle_returns409() {
    SignupRequest request = new SignupRequest("exampleHandle", "password", null, null, null);
    testRestTemplate.postForEntity("/api/auth/signup", request, AuthResponse.class);

    ResponseEntity<ErrorResponse> response =
        testRestTemplate.postForEntity("/api/auth/signup", request, ErrorResponse.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
  }

  @Test
  void signup_invalidRequest_returns400() {
    SignupRequest request = new SignupRequest("exampleHandle?", "password", null, null, null);

    ResponseEntity<ValidationErrorResponse> response =
        testRestTemplate.postForEntity("/api/auth/signup", request, ValidationErrorResponse.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
  }

  @Test
  void login_validRequest_returns200AndSetsRefreshCookie() {
    signup();
    LoginRequest request = new LoginRequest("exampleHandle", "password");
    ResponseEntity<AuthResponse> response =
        testRestTemplate.postForEntity("/api/auth/login", request, AuthResponse.class);


    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getHeaders().get("Set-Cookie"))
        .anyMatch(cookie -> cookie.matches("refreshToken=" + refreshTokenRegex + ";.*"));
    Assertions.assertNotNull(response.getBody());
    assertThat(response.getBody().accessToken()).isNotNull();
  }

  @Test
  void login_validRequest_persistsUserToDatabase() {
    signup();
    LoginRequest request = new LoginRequest("exampleHandle", "password");
    testRestTemplate.postForEntity("/api/auth/login", request, AuthResponse.class);

    // User persisted
    List<User> users = userRepository.findAll();
    assertThat(users).hasSize(1);
    assertThat(users.getFirst().getHandle()).isEqualTo("exampleHandle");
  }

  @Test
  void login_validRequest_storesRefreshTokensInRedis() {
    signup();
    LoginRequest request = new LoginRequest("exampleHandle", "password");
    testRestTemplate.postForEntity("/api/auth/login", request, AuthResponse.class);

    // Refresh tokens stored in redis (one for signup, one for login)
    Set<String> keys = stringRedisTemplate.keys("refresh_token:*");
    assertThat(keys).hasSize(2);
  }

  @Test
  void login_invalidCredentials_returns401() {
    signup();
    LoginRequest request = new LoginRequest("exampleHandle", "passwordDoesNotMatch");
    ResponseEntity<ErrorResponse> response =
        testRestTemplate.postForEntity("/api/auth/login", request, ErrorResponse.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
  }

  @Test
  void login_invalidRequest_returns400() {
    signup();
    LoginRequest request = new LoginRequest("exampleHandle?", "password");
    ResponseEntity<ValidationErrorResponse> response =
        testRestTemplate.postForEntity("/api/auth/login", request, ValidationErrorResponse.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
  }

  @Test
  void logout_validRequest_returns204AndClearsRefreshCookie() {
    ResponseEntity<AuthResponse> signupResponse = signup();
    String accessToken = signupResponse.getBody().accessToken();
    String refreshToken = extractRefreshCookie(signupResponse);

    HttpHeaders headers = new HttpHeaders();
    headers.add("Authorization", "Bearer " + accessToken);
    headers.add("Cookie", refreshToken);
    ResponseEntity<Void> response =
        testRestTemplate.postForEntity("/api/auth/logout", new HttpEntity<>(headers), Void.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    assertThat(response.getHeaders().get("Set-Cookie"))
        .anyMatch(cookie -> cookie.matches("refreshToken=;.*"));
  }

  @Test
  void logout_validRequest_removesRefreshTokenInRedis() {
    ResponseEntity<AuthResponse> signupResponse = signup();
    String accessToken = signupResponse.getBody().accessToken();
    String refreshToken = extractRefreshCookie(signupResponse);

    HttpHeaders headers = new HttpHeaders();
    headers.add("Authorization", "Bearer " + accessToken);
    headers.add("Cookie", refreshToken);
    ResponseEntity<Void> response =
        testRestTemplate.postForEntity("/api/auth/logout", new HttpEntity<>(headers), Void.class);

    Set<String> keys = stringRedisTemplate.keys("refresh_token:*");
    assertThat(keys).hasSize(0);
  }

  @Test
  void logout_invalidRefreshToken_returns401() {
    ResponseEntity<AuthResponse> signupResponse = signup();
    String accessToken = signupResponse.getBody().accessToken();
    String invalidRefreshToken = UUID.randomUUID().toString();

    HttpHeaders headers = new HttpHeaders();
    headers.add("Authorization", "Bearer " + accessToken);
    headers.add("Cookie", "refreshToken=" + invalidRefreshToken);
    ResponseEntity<Void> response =
        testRestTemplate.postForEntity("/api/auth/logout", new HttpEntity<>(headers), Void.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
  }

  @Test
  void refresh_validRequest_returns200WithNewAccessToken() {
    ResponseEntity<AuthResponse> signupResponse = signup();
    String refreshCookie = extractRefreshCookie(signupResponse);


    HttpHeaders headers = new HttpHeaders();
    headers.add("Cookie", refreshCookie);
    ResponseEntity<AuthResponse> response =
        testRestTemplate.postForEntity("/api/auth/refresh", new HttpEntity<>(headers),
            AuthResponse.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    Assertions.assertNotNull(response.getBody());
    assertThat(response.getBody().accessToken()).isNotNull();
  }

  @Test
  void refresh_validRequest_setsNewRefreshCookie() {
    ResponseEntity<AuthResponse> signupResponse = signup();
    String refreshCookie = extractRefreshCookie(signupResponse);

    HttpHeaders headers = new HttpHeaders();
    headers.add("Cookie", refreshCookie);
    ResponseEntity<AuthResponse> response =
        testRestTemplate.postForEntity("/api/auth/refresh", new HttpEntity<>(headers),
            AuthResponse.class);

    assertThat(response.getHeaders().get("Set-Cookie"))
        .anyMatch(cookie -> cookie.matches("refreshToken=" + refreshTokenRegex + ";.*"));
  }

  @Test
  void refresh_validRequest_rotatesRefreshTokenInRedis() {
    ResponseEntity<AuthResponse> signupResponse = signup();
    String oldRefreshToken = extractRefreshCookie(signupResponse);
    String oldRefreshTokenValue = oldRefreshToken.replace("refreshToken=", "");

    Set<String> oldKeys = stringRedisTemplate.keys("refresh_token:*");
    assertThat(oldKeys).hasSize(1);
    assertThat(oldKeys).contains("refresh_token:" + oldRefreshTokenValue);

    HttpHeaders headers = new HttpHeaders();
    headers.add("Cookie", oldRefreshToken);
    ResponseEntity<AuthResponse> response =
        testRestTemplate.postForEntity("/api/auth/refresh", new HttpEntity<>(headers),
            AuthResponse.class);

    List<String> cookies = response.getHeaders().get("Set-Cookie");
    assertThat(cookies).isNotNull().hasSize(1)
        .anyMatch(cookie -> cookie.matches("refreshToken=" + refreshTokenRegex + ";.*"));
    String newRefreshToken = cookies.getFirst().split(";")[0];
    String newRefreshTokenValue = newRefreshToken.replace("refreshToken=", "");


    Set<String> newKeys = stringRedisTemplate.keys("refresh_token:*");
    assertThat(newKeys).hasSize(1);
    assertThat(newKeys).contains("refresh_token:" + newRefreshTokenValue);
  }

  @Test
  void refresh_invalidRefreshToken_returns401() {
    String invalidRefreshToken = UUID.randomUUID().toString();
    HttpHeaders headers = new HttpHeaders();
    headers.add("Cookie", "refreshToken=" + invalidRefreshToken);
    ResponseEntity<ErrorResponse> response =
        testRestTemplate.postForEntity("/api/auth/refresh", new HttpEntity<>(headers),
            ErrorResponse.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
  }

  @Test
  void refresh_inactiveAccount_returns403() {
    ResponseEntity<AuthResponse> signupResponse = signup();
    String refreshCookie = extractRefreshCookie(signupResponse);

    // Simulate account suspension directly via repository
    User user = userRepository.findByHandle("exampleHandle").orElseThrow();
    user.setStatus(UserStatus.SUSPENDED);
    userRepository.save(user);

    HttpHeaders headers = new HttpHeaders();
    headers.add("Cookie", refreshCookie);
    ResponseEntity<ErrorResponse> response =
        testRestTemplate.postForEntity("/api/auth/refresh", new HttpEntity<>(headers),
            ErrorResponse.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
  }
}
