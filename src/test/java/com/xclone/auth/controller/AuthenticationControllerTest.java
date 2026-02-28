package com.xclone.auth.controller;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xclone.auth.dto.AuthResponse;
import com.xclone.auth.dto.AuthTokens;
import com.xclone.auth.dto.LoginRequest;
import com.xclone.auth.dto.SignupRequest;
import com.xclone.auth.service.AuthenticationService;
import com.xclone.config.AuthProperties;
import com.xclone.exception.custom.AccountNotActiveException;
import com.xclone.exception.custom.DuplicateHandleException;
import com.xclone.exception.custom.InvalidRefreshTokenException;
import com.xclone.security.config.SecurityConfig;
import com.xclone.security.jwt.JwtTokenProvider;
import com.xclone.security.user.JwtUserDetailsService;
import jakarta.servlet.http.Cookie;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = AuthenticationController.class)
@Import({SecurityConfig.class, AuthProperties.class})
@ActiveProfiles("dev")
public class AuthenticationControllerTest {
  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @MockitoBean
  private AuthenticationService authenticationService;

  // JwtAuthenticationFilter's dependencies
  @MockitoBean
  private JwtTokenProvider jwtTokenProvider;
  @MockitoBean
  private JwtUserDetailsService userDetailsService;

  private String refreshTokenId;
  private AuthTokens validAuthTokens;
  private AuthResponse validAuthResponse;

  @BeforeEach
  void setUp() {
    refreshTokenId = UUID.randomUUID().toString();
    String accessToken = UUID.randomUUID().toString();
    String userId = UUID.randomUUID().toString();
    validAuthTokens = new AuthTokens(refreshTokenId, accessToken, userId, "exampleDisplayName",
        "exampleProfileUrl");
    validAuthResponse = validAuthTokens.toAuthResponse();
  }

  // Helper
  private String toJson(Object obj) throws Exception {
    return objectMapper.writeValueAsString(obj);
  }

  @Test
  void signup_validRequest_returns200WithRefreshTokenCookie() throws Exception {
    // Setup
    SignupRequest validSignup = new SignupRequest("exampleHandle", "password", null, null, null);

    when(authenticationService.signup(validSignup)).thenReturn(validAuthTokens);

    // Act
    mockMvc.perform(post("/api/auth/signup")
            .contentType(MediaType.APPLICATION_JSON)
            .content(toJson(validSignup)))
        .andExpect(status().isOk())
        .andExpect(cookie().value("refreshToken", refreshTokenId))
        .andExpect(content().json(toJson(validAuthResponse)));
  }

  @Test
  void signup_duplicateHandle_returns409() throws Exception {
    // Setup
    SignupRequest validSignup = new SignupRequest("exampleHandle", "password", null, null, null);

    when(authenticationService.signup(validSignup)).thenThrow(
        new DuplicateHandleException("This handle is already taken"));

    // Act
    mockMvc.perform(post("/api/auth/signup")
            .contentType(MediaType.APPLICATION_JSON)
            .content(toJson(validSignup)))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.message").value("This handle is already taken"));
  }

  @Test
  void signup_invalidRequestWithOneFieldError_returns400() throws Exception {
    // Setup
    SignupRequest invalidHandle =
        new SignupRequest("invalidHandle//?", "password", null, null, null);

    // Act
    mockMvc.perform(post("/api/auth/signup")
            .contentType(MediaType.APPLICATION_JSON)
            .content(toJson(invalidHandle)))
        .andExpect(jsonPath("$.errors").isArray())
        .andExpect(jsonPath("$.errors.length()").value(1))
        .andExpect(jsonPath("$.errors[0].field").value("handle"));
  }

  @Test
  void signup_invalidRequestWithMultipleFieldError_returns400() throws Exception {
    // Setup
    SignupRequest invalidPassword =
        new SignupRequest("handle", "123_?", null, null, null);

    // Act
    mockMvc.perform(post("/api/auth/signup")
            .contentType(MediaType.APPLICATION_JSON)
            .content(toJson(invalidPassword)))
        .andExpect(jsonPath("$.errors").isArray())
        .andExpect(jsonPath("$.errors.length()").value(2))
        // Invalid size
        .andExpect(jsonPath("$.errors[0].field").value("password"))
        // Invalid regex
        .andExpect(jsonPath("$.errors[1].field").value("password"));
  }

  @Test
  void login_validRequest_returns200WithRefreshTokenCookie() throws Exception {
    // Setup
    LoginRequest validLogin = new LoginRequest("exampleHandle", "password");

    when(authenticationService.login(validLogin)).thenReturn(validAuthTokens);

    // Act
    mockMvc.perform(post("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(toJson(validLogin)))
        .andExpect(status().isOk())
        .andExpect(cookie().value("refreshToken", refreshTokenId))
        .andExpect(content().json(toJson(validAuthResponse)));
  }

  @Test
  void login_invalidCredentials_returns401() throws Exception {
    // Setup
    LoginRequest validLogin = new LoginRequest("exampleHandle", "password");

    when(authenticationService.login(validLogin)).thenThrow(
        new BadCredentialsException("Invalid credentials"));

    // Act
    mockMvc.perform(post("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(toJson(validLogin)))
        .andExpect(status().isUnauthorized());
  }


  @Test
  void login_invalidRequestWithMultipleFieldError_returns400() throws Exception {
    // Setup
    LoginRequest invalidPassword = new LoginRequest("exampleHandle", "123_?");
    // Act
    mockMvc.perform(post("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(toJson(invalidPassword)))
        .andExpect(jsonPath("$.errors").isArray())
        .andExpect(jsonPath("$.errors.length()").value(2))
        // Invalid size
        .andExpect(jsonPath("$.errors[0].field").value("password"))
        // Invalid regex
        .andExpect(jsonPath("$.errors[1].field").value("password"));
  }

  @Test
  void logout_validRequest_returns204() throws Exception {
    // Setup
    String token = "api-key-value";

    // Act
    mockMvc.perform(post("/api/auth/logout")
            .contentType(MediaType.APPLICATION_JSON)
            .cookie(new Cookie("refreshToken", refreshTokenId))
            .header("Authorization", "Bearer " + token))
        .andExpect(status().isNoContent())
        .andExpect(cookie().value("refreshToken", ""));

    // Assert
    verify(authenticationService, times(1)).logout(token, refreshTokenId);
  }

  @Test
  void logout_invalidRefreshToken_returns401() throws Exception {
    // Setup
    String token = "api-key-value";
    doThrow(new InvalidRefreshTokenException("Invalid refresh token")).when(authenticationService)
        .logout(token, refreshTokenId);

    // Act
    mockMvc.perform(post("/api/auth/logout")
            .contentType(MediaType.APPLICATION_JSON)
            .cookie(new Cookie("refreshToken", refreshTokenId))
            .header("Authorization", "Bearer " + token))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.message").value("Invalid refresh token"));
  }


  @Test
  void refresh_validRequest_returns200WithRefreshTokenCookie() throws Exception {
    // Setup
    String refreshTokenIdRequest = UUID.randomUUID().toString();
    when(this.authenticationService.refresh(refreshTokenIdRequest)).thenReturn(validAuthTokens);

    // Act
    mockMvc.perform(post("/api/auth/refresh")
            .contentType(MediaType.APPLICATION_JSON)
            .cookie(new Cookie("refreshToken", refreshTokenIdRequest)))
        .andExpect(status().isOk())
        .andExpect(cookie().value("refreshToken", refreshTokenId))
        .andExpect(content().json(toJson(validAuthResponse)));

    // Assert
    verify(authenticationService, times(1)).refresh(refreshTokenIdRequest);
  }

  @Test
  void refresh_invalidRefreshToken_returns401() throws Exception {
    // Setup
    String refreshTokenIdRequest = UUID.randomUUID().toString();
    when(authenticationService.refresh(refreshTokenIdRequest)).thenThrow(
        new InvalidRefreshTokenException("Invalid refresh token"));

    // Act
    mockMvc.perform(post("/api/auth/refresh")
            .contentType(MediaType.APPLICATION_JSON)
            .cookie(new Cookie("refreshToken", refreshTokenIdRequest)))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.message").value("Invalid refresh token"));
  }

  @Test
  void refresh_invalidUser_returns401() throws Exception {
    // Setup
    String refreshTokenIdRequest = UUID.randomUUID().toString();
    when(authenticationService.refresh(refreshTokenIdRequest)).thenThrow(
        new UsernameNotFoundException("User not found"));

    // Act
    mockMvc.perform(post("/api/auth/refresh")
            .contentType(MediaType.APPLICATION_JSON)
            .cookie(new Cookie("refreshToken", refreshTokenIdRequest)))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.message").value("User not found"));
  }

  @Test
  void refresh_invalidUserStatus_returns403() throws Exception {
    // Setup
    String refreshTokenIdRequest = UUID.randomUUID().toString();
    when(authenticationService.refresh(refreshTokenIdRequest)).thenThrow(
        new AccountNotActiveException("Account not active"));

    // Act
    mockMvc.perform(post("/api/auth/refresh")
            .contentType(MediaType.APPLICATION_JSON)
            .cookie(new Cookie("refreshToken", refreshTokenIdRequest)))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.message").value("Account not active"));
  }
}
