package com.xclone.auth.dto;

/**
 * DTO which contains auth tokens.
 *
 * @param refreshToken
 * @param accessToken
 * @param userId
 * @param displayName
 * @param handle
 */
public record AuthTokens(
    String refreshToken,
    String accessToken,
    String userId,
    String displayName,
    String handle
) {

  /**
   * Converts {@link AuthTokens} to {@link AuthResponse} DTO.
   *
   * @return {@link AuthResponse}
   */
  public AuthResponse toAuthResponse() {
    return new AuthResponse(
        this.accessToken(),
        this.userId(),
        this.displayName(),
        this.handle()
    );
  }
}
