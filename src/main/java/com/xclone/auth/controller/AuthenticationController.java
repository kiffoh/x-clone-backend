package com.xclone.auth.controller;

import com.xclone.auth.dto.AuthResponse;
import com.xclone.auth.dto.AuthTokens;
import com.xclone.auth.dto.LoginRequest;
import com.xclone.auth.service.AuthenticationService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Router for authentication REST API calls.
 */
@RestController
@RequestMapping("api/auth")
public class AuthenticationController {
  private final AuthenticationService authenticationService;

  AuthenticationController(AuthenticationService authenticationService) {
    this.authenticationService = authenticationService;
  }

  //      // All routes will have /auth here - can I set this at a higher level?
  //      // How is validation done in Spring Boot? Is there something like zod (validation
  //    // middleware) which can be used so that I can treat the values as non null?
  //      @PostMapping("/sign-up")
  //      public User createUser(HttpServletRequest request, HttpServletResponse response) {
  //          // Take the information username and password
  //          -> hash the password and pass it to the datbase.
  //          // What is the industry standard, to hash on server side?
  //          Otherwise we are trusting the client information?

  //          String handle = Arrays.toString(request.getParameterValues("handle"));
  //          if (handleIsUnique(handle)) {
  //              String password = Arrays.toString(request.getParameterValues("password"));
  //              User user = new User();
  //              user.setHandle(handle);
  //              // Can I encode the password with passwordEncoder in SecurityConfig?
  //              user.setPasswordHash(password); // Will have to encrypt at some point
  //              userRepository.save(user);
  //              return user;
  //          }
  //          throw new IllegalAccessError("Not a unique handle"); // Not unique error
  //      }

  //      public boolean handleIsUnique(String handle) {
  //          User user = new User();
  //          user.setHandle(handle);
  //          return userRepository.exists(user);
  //      }

  /**
   * Controller layer for login requests.
   * Calls the relevant controllers to validate and log a user in.
   * Assigns a new access and refresh token as part of response.
   *
   * @param request  HTTP request object
   * @param response HTTP response object
   * @return {@link AuthResponse} dto
   */
  @PostMapping("/login")
  public ResponseEntity<AuthResponse> logIn(
      @RequestBody @Valid LoginRequest request,
      HttpServletResponse response) {
    AuthTokens authTokens = authenticationService.login(request);
    response.addCookie(new Cookie("refreshToken", authTokens.refreshToken()));
    return ResponseEntity.ok(authTokens.toAuthResponse());
  }

  //      @PostMapping("/log-out")
  //      public void logOut(HttpServletRequest request, HttpServletResponse response) {
  //          AccessToken token = SecurityContextHolder.getContext().getAuthentication();
  //          // Send an empty response which also resets the clients http cookies
  //      }
}
