package com.xclone.security.dto;

/**
 * DTO which contains auth tokens.
 *
 * @param refreshToken
 * @param accessToken
 * @param userId
 * @param displayName
 * @param handle
 */
public record AuthServiceDto(
    String refreshToken,
    String accessToken,
    String userId,
    String displayName,
    String handle
) {

  /**
   * Converts {@link AuthServiceDto} to {@link AuthResponse} DTO.
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
