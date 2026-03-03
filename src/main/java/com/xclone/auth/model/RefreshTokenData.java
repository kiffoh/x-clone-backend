package com.xclone.auth.model;

import com.xclone.config.AuthProperties;
import java.time.Instant;

/**
 * Represents metadata associated with a refresh token, including its owner and lifecycle
 * timestamps.
 *
 * @param userId the unique identifier of the user the token belongs to
 * @param createdAt the instant the refresh token was created
 * @param expiresAt the instant the refresh token expires
 */
public record RefreshTokenData(String userId, Instant createdAt, Instant expiresAt) {

  /**
   * Creates a new {@code RefreshTokenData} instance using the configured refresh token duration.
   *
   * @param userId the unique identifier of the user
   * @param authProperties configuration containing refresh token settings
   * @return a new refresh token metadata instance
   */
  public static RefreshTokenData create(String userId, AuthProperties authProperties) {
    Instant now = Instant.now();
    return new RefreshTokenData(
        userId, now, now.plusSeconds(authProperties.getRefreshTokenDurationSeconds()));
  }

  /**
   * Determines whether the refresh token has expired.
   *
   * @return {@code true} if the current time is after {@code expiresAt}; otherwise {@code false}
   */
  public boolean isExpired() {
    return Instant.now().isAfter(expiresAt);
  }
}
