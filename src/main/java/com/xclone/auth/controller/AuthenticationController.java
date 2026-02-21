package com.xclone.auth.controller;

import com.xclone.auth.dto.AuthResponse;
import com.xclone.auth.dto.AuthTokens;
import com.xclone.auth.dto.LoginRequest;
import com.xclone.auth.dto.SignupRequest;
import com.xclone.auth.service.AuthenticationService;
import com.xclone.config.AuthProperties;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Router for authentication REST API calls.
 */
@RestController
@RequestMapping("api/auth")
@Slf4j
public class AuthenticationController {
  private final AuthenticationService authenticationService;
  private final AuthProperties authProperties;

  AuthenticationController(AuthenticationService authenticationService,
                           AuthProperties authProperties) {
    this.authenticationService = authenticationService;
    this.authProperties = authProperties;
  }

  /**
   * Controller layer for signup requests.
   * Calls the signup service function to validate and register a user.
   * Assigns a new access and refresh token as part of response.
   *
   * @param request  HTTP request object
   * @param response HTTP response object
   * @return {@link AuthResponse} dto
   */
  @PostMapping("/signup")
  public ResponseEntity<AuthResponse> signup(
      @RequestBody @Valid SignupRequest request,
      HttpServletResponse response) {
    AuthTokens authTokens = authenticationService.signup(request);
    setRefreshTokenCookie(response, authTokens.refreshToken());
    return ResponseEntity.ok(
        authTokens.toAuthResponse());
  }

  /**
   * Controller layer for login requests.
   * Calls the login service function to validate and log a user in.
   * Assigns a new access and refresh token as part of response.
   *
   * @param request  HTTP request object
   * @param response HTTP response object
   * @return {@link AuthResponse} dto
   */
  @PostMapping("/login")
  public ResponseEntity<AuthResponse> login(
      @RequestBody @Valid LoginRequest request,
      HttpServletResponse response) {
    AuthTokens authTokens = authenticationService.login(request);
    setRefreshTokenCookie(response, authTokens.refreshToken());
    return ResponseEntity.ok(authTokens.toAuthResponse());
  }

  /**
   * Controller layer for logout requests.
   * Calls the logout service function to validate and log out a user.
   * Overwrites the existing refresh token as part of response.
   *
   * @param response HTTP response object
   */
  @PostMapping("/logout")
  public ResponseEntity<Void> logout(
      @CookieValue("refreshToken") String refreshToken,
      @RequestHeader("Authorization") String authHeaders,
      HttpServletResponse response
  ) {
    log.info("Logout request received");
    String accessToken = authHeaders.replace("Bearer ", "");
    authenticationService.logout(accessToken, refreshToken);
    clearRefreshTokenCookie(response);

    log.info("Logout successful");
    return ResponseEntity.noContent().build();
  }

  /**
   * Controller layer for refresh requests.
   * Calls the logout service function to validate and log out a user.
   * Overwrites the existing refresh token as part of response.
   *
   * @param response HTTP response object
   */
  @PostMapping("/refresh")
  public ResponseEntity<AuthResponse> refresh(
      @CookieValue("refreshToken") String refreshToken,
      HttpServletResponse response
  ) {
    AuthTokens authTokens = authenticationService.refresh(refreshToken);
    String newRefreshTokenId = authTokens.refreshToken();
    setRefreshTokenCookie(response, newRefreshTokenId);
    return ResponseEntity.ok(authTokens.toAuthResponse());
  }

  private void setRefreshTokenCookie(HttpServletResponse response, String refreshToken) {
    ResponseCookie cookie = ResponseCookie.from("refreshToken", refreshToken)
        .httpOnly(true)
        .secure(this.authProperties.isSecureCookies())
        .path("/api/auth")
        .maxAge(authProperties.getRefreshTokenDurationSeconds())
        .sameSite("Strict")
        .build();

    response.addHeader("Set-Cookie", cookie.toString());
  }

  private void clearRefreshTokenCookie(HttpServletResponse response) {
    ResponseCookie cookie = ResponseCookie.from("refreshToken", "")
        .httpOnly(true)
        .secure(this.authProperties.isSecureCookies())
        .path("/api/auth")
        .maxAge(0)
        .sameSite("Strict")
        .build();

    response.addHeader("Set-Cookie", cookie.toString());
  }


}
