package com.xclone.auth.dto;

/**
 * DTO which contains auth tokens.
 *
 * @param refreshToken string UUID used as the key in the redis instance for the token metadata
 * @param accessToken  a jwt containing information used to validate the user on the server-side
 * @param userId       string UUID unique to the user
 * @param displayName  selected name by user
 * @param handle       unique string associated to the user
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
