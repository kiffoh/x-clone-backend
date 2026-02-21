package com.xclone.auth.service;

import com.xclone.auth.model.RefreshTokenData;
import com.xclone.auth.repository.RefreshTokenRepository;
import com.xclone.config.AuthProperties;
import com.xclone.exception.custom.InvalidRefreshTokenException;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * Provides redis CRUD for refresh tokens.
 */
@Service
public class RefreshTokenService {
  private final RefreshTokenRepository refreshTokenRepository;
  private final AuthProperties authProperties;

  public RefreshTokenService(RefreshTokenRepository refreshTokenRepository,
                             AuthProperties authProperties) {
    this.refreshTokenRepository = refreshTokenRepository;
    this.authProperties = authProperties;
  }

  private String generateOpaqueKey() {
    return UUID.randomUUID().toString();
  }

  /**
   * Creates a UUID string (tokenId) and stores token metadata ({@link RefreshTokenData}) in Redis.
   * TODO: to refactor this javadoc comment.
   *
   * @return UUID string (tokenId)
   */
  public String createToken(String userId) {
    String tokenId = generateOpaqueKey();
    RefreshTokenData metadata = RefreshTokenData.create(userId, this.authProperties);
    this.refreshTokenRepository.save(tokenId, metadata);
    return tokenId;
  }

  public RefreshTokenData getToken(String tokenId) {
    return refreshTokenRepository.find(tokenId);
  }


  public String rotateToken(String tokenId) {
    RefreshTokenData token = this.refreshTokenRepository.find(tokenId);
    if (token.isExpired()) {
      throw new InvalidRefreshTokenException("Refresh token is invalid");
    }
    String newTokenId = generateOpaqueKey();
    RefreshTokenData metadata = RefreshTokenData.create(token.userId(), authProperties);
    this.refreshTokenRepository.save(newTokenId, metadata);
    removeToken(tokenId);
    return newTokenId;
  }

  public void removeToken(String refreshTokenId) {
    this.refreshTokenRepository.delete(refreshTokenId);
  }
}