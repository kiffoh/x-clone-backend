package com.xclone.auth.model;

import com.xclone.config.AuthProperties;
import java.time.Instant;

public record RefreshTokenData(String userId, Instant createdAt,
                               Instant expiresAt) {
  public static RefreshTokenData create(String userId,
                                        AuthProperties authProperties) {
    Instant now = Instant.now();
    return new RefreshTokenData(
        userId,
        now,
        now.plusSeconds(authProperties.getRefreshTokenDurationSeconds()));
  }

  public boolean isExpired() {
    return Instant.now().isAfter(expiresAt);
  }
}
