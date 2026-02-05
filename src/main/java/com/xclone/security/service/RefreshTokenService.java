package com.xclone.security.service;

import org.springframework.stereotype.Service;

/**
 * Redis CRUD for refresh tokens.
 */
@Service
public class RefreshTokenService {

  public String createToken() {
    return "Implementation will come soon";
  }


//  /**
//   * Generates and validates refresh tokens.
//   */
//  public RefreshToken generateToken(String userId, String deviceInfo) {
//    final int TOKEN_DURATION = (60 * 60 * 24 * 30 * 1000);
//    Date createdAt = new Date();
//    Date expiresAt = new Date(createdAt.getTime() + TOKEN_DURATION);
//    return new RefreshToken(userId, createdAt, expiresAt, deviceInfo);
//  }
}