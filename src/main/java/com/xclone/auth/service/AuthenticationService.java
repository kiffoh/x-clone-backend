package com.xclone.auth.service;

import com.xclone.auth.dto.AuthTokens;
import com.xclone.auth.dto.LoginRequest;
import com.xclone.security.jwt.JwtTokenProvider;
import com.xclone.user.model.entity.User;
import com.xclone.user.repository.UserRepository;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Coordinates business layers to perform route logic.
 */
@Service
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
  public AuthTokens login(LoginRequest request) {
    // Get user from database. Create access token from user information and return access token?

    // Get full User entity
    User user = this.userRepository.findByHandle(request.handle()).orElseThrow(() ->
        new BadCredentialsException("Invalid credentials"));

    // Verify password
    if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
      throw new BadCredentialsException("Invalid credentials");
    }


    String accessToken =
        jwtTokenProvider.createToken(
            user.getId().toString(),
            user.getRole().toString()
        );
    String refreshToken = refreshTokenService.createToken();
    return new AuthTokens(
        accessToken,
        refreshToken,
        user.getId().toString(),
        user.getDisplayName(),
        user.getProfileImage()
    );
  }
}
