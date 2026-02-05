package com.xclone.security.dto;

/**
 * DTO to return authentication data to client.
 *
 * @param accessToken
 * @param userId
 * @param displayName
 * @param handle
 */
public record AuthResponse(
    String accessToken,
    String userId,
    String displayName,
    String handle
) {
}
