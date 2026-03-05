package com.xclone.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Response payload returned after successful authentication operations (signup, login, refresh).
 *
 * @param accessToken  signed JWT access token used for authenticated API requests
 * @param userId       UUID of the authenticated user
 * @param displayName  display name visible to other users
 * @param profileImage URI of the user's profile image
 */
public record AuthResponse(
    @Schema(
        description =
            "Short-lived JWT used to authenticate subsequent requests. "
                + "Include in the Authorization header as: Bearer {token}.")
    String accessToken,
    @Schema(description = "Unique identifier of the authenticated user.") String userId,
    @Schema(description = "Display name shown on the user's profile.") String displayName,
    @Schema(description = "Profile image URL.") String profileImage) {
}
