package com.xclone.auth.service;

import com.xclone.auth.dto.AuthTokens;
import com.xclone.auth.dto.LoginRequest;
import com.xclone.auth.dto.SignupRequest;
import com.xclone.auth.model.RefreshTokenData;
import com.xclone.exception.custom.AccountNotActiveException;
import com.xclone.exception.custom.DuplicateHandleException;
import com.xclone.exception.custom.InvalidRefreshTokenException;
import com.xclone.security.jwt.JwtTokenProvider;
import com.xclone.user.model.entity.User;
import com.xclone.user.model.enums.UserStatus;
import com.xclone.user.repository.UserRepository;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.CookieValue;

/**
 * Coordinates business layers to perform route logic.
 */
@Service
@Slf4j
public class AuthenticationService {
  private final JwtTokenProvider jwtTokenProvider;
  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final RefreshTokenService refreshTokenService;

  public AuthenticationService(JwtTokenProvider jwtTokenProvider,
                               UserRepository userRepository,
                               PasswordEncoder passwordEncoder,
                               RefreshTokenService refreshTokenService
  ) {
    this.jwtTokenProvider = jwtTokenProvider;
    this.userRepository = userRepository;
    this.passwordEncoder = passwordEncoder;
    this.refreshTokenService = refreshTokenService;
  }

  /**
   * Gets user from database.
   * Returns {@link AuthTokens} which contains a valid access token and refresh token.
   */
  public AuthTokens login(@Valid LoginRequest request) {
    log.info("login service called. Validating request.");
    User user = this.userRepository.findByHandle(request.handle()).orElseThrow(() -> {
      log.warn("login attempted for a user does not exist");
      return new BadCredentialsException("Invalid credentials");
    });

    // Verify password
    if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
      log.warn("login attempted for with an invalid password");
      throw new BadCredentialsException("Invalid credentials");
    }


    String accessToken =
        jwtTokenProvider.createToken(
            user.getId().toString(),
            user.getRole().toString()
        );
    String refreshToken = refreshTokenService.createToken(user.getId().toString());
    log.info("login successful for user {}", user.getId());
    return new AuthTokens(
        refreshToken,
        accessToken,
        user.getId().toString(),
        user.getDisplayName(),
        user.getProfileImage()
    );
  }

  public AuthTokens signup(@Valid SignupRequest request) {
    if (userRepository.existsByHandle(request.handle())) {
      log.warn("signup attempt with an existing handle");
      throw new DuplicateHandleException(
          "This handle is already taken"); // Is this the correct error?
    }

    // Create new User
    User newUser = new User();
    newUser.setHandle(request.handle());
    newUser.setPasswordHash(passwordEncoder.encode(request.password()));
    // Display name is defaulted to handle if not provided
    if (request.displayName() == null) {
      log.info("display name is set as the handle as display name not provided");
      newUser.setDisplayName(request.handle());
    } else {
      log.info("display name is set as {}", request.displayName());
      newUser.setDisplayName(request.displayName());
    }
    User savedUser = userRepository.save(newUser);

    // Create access token
    String accessToken =
        jwtTokenProvider.createToken(
            savedUser.getId().toString(),
            savedUser.getRole().toString()
        );

    // Create refresh token
    String refreshToken = refreshTokenService.createToken(savedUser.getId().toString());
    log.info("signup successful for user {}", savedUser.getId());
    return new AuthTokens(
        refreshToken,
        accessToken,
        savedUser.getId().toString(),
        savedUser.getDisplayName(),
        savedUser.getProfileImage()
    );
  }

  public void logout(String accessToken, String refreshTokenId) {
    log.debug("logout service called. Validating logout request");
    RefreshTokenData tokenData = refreshTokenService.getToken(refreshTokenId);
    // Check if token expired
    if (tokenData == null || tokenData.isExpired()) {
      log.warn("Logout attempted with an invalid refresh token");
      throw new InvalidRefreshTokenException("Invalid refresh token");
    }

    // Confirm if user id in tokens match
    String userId = jwtTokenProvider.getUserIdFromToken(accessToken);
    if (!userId.equals(tokenData.userId())) {
      log.error(
          "SECURITY: Token mismatch during logout " +
              "- accessToken userId: {}, refreshToken userId: {}",
          userId, tokenData.userId());

      throw new InvalidRefreshTokenException("Invalid refresh token");
    }

    // Remove token once proven to be valid and secure
    refreshTokenService.removeToken(refreshTokenId);
    log.info("User {} logged out successfully", userId);
  }

  public AuthTokens refresh(@CookieValue("refreshToken") String refreshTokenId) {
    log.debug("refresh service called. Validating refresh request");
    RefreshTokenData tokenData = refreshTokenService.getToken(refreshTokenId);
    // Check if token expired
    if (tokenData == null || tokenData.isExpired()) {
      log.warn("refresh token attempted with an invalid refresh token");
      throw new InvalidRefreshTokenException("Invalid refresh token");
    }

    // Validates user exists for security
    String userId = tokenData.userId();
    User user = userRepository.findById(UUID.fromString(userId))
        .orElseThrow(() -> {
          log.warn("Refresh attempted for non-existent user: {}", userId);
          // Clean up orphaned token
          refreshTokenService.removeToken(refreshTokenId);
          return new UsernameNotFoundException("User not found");
        });
    // TODO: Check if user_tokens:user_id contains the refreshTokenId for security

    // Check user status (user status has SUSPENDED and DELETED statuses)
    if (user.getStatus() != UserStatus.ACTIVE) {
      log.warn("Refresh attempted for {} user: {}", user.getStatus(), userId);
      refreshTokenService.removeToken(refreshTokenId);
      throw new AccountNotActiveException("Account not active");
    }

    // Rotate token once validated
    String newRefreshTokenId = refreshTokenService.rotateToken(refreshTokenId);
    // Create access token
    String newAccessToken =
        jwtTokenProvider.createToken(
            userId,
            user.getRole().toString()
        );
    log.info("refreshToken rotated successfully for user {}", userId);
    return new AuthTokens(
        newRefreshTokenId,
        newAccessToken,
        userId,
        user.getDisplayName(),
        user.getProfileImage()
    );
  }
}
