package com.xclone.auth.dto;

/**
 * Response payload returned after successful authentication operations (signup, login, refresh).
 *
 * @param accessToken signed JWT access token used for authenticated API requests
 * @param userId UUID of the authenticated user
 * @param displayName display name visible to other users
 * @param profileImage URI of the user's profile image
 */
public record AuthResponse(
    String accessToken, String userId, String displayName, String profileImage) {}
