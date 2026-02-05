package com.xclone.security.service;

import com.xclone.model.entity.User;
import com.xclone.repository.UserRepository;
import com.xclone.security.JwtTokenProvider;
import com.xclone.security.dto.AuthServiceDto;
import com.xclone.security.dto.LoginRequest;
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
   * Returns {@link AuthServiceDto} which contains a valid access token and refresh token.
   */
  public AuthServiceDto login(LoginRequest request) {
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
    return new AuthServiceDto(
        accessToken,
        refreshToken,
        user.getId().toString(),
        user.getDisplayName(),
        user.getProfileImage()
    );
  }
}
