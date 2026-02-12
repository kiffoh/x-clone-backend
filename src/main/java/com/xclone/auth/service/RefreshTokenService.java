package com.xclone.auth.service;

import com.xclone.auth.model.RefreshTokenData;
import org.springframework.stereotype.Service;

/**
 * Provides redis CRUD for refresh tokens.
 */
@Service
public class RefreshTokenService {

  /**
   * Creates a UUID string (tokenId) and stores token metadata ({@link RefreshTokenData}) in Redis.
   * TODO: to refactor this javadoc comment.
   *
   * @return UUID string (tokenId)
   */
  public String createToken() {
    return "Implementation will come soon";
  }

  public void removeToken(String refreshTokenId) {
  }

  public boolean tokenValid(RefreshTokenData refreshToken) {
  }

  public String rotateToken(String userId) {
    return null;
  }

  public String getUserId(String refreshToken) {
  }

  public RefreshTokenData getTokenData(String refreshToken) {
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