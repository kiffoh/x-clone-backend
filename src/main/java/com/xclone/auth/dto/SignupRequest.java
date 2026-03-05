package com.xclone.auth.dto;

import com.xclone.validation.ValidHandle;
import com.xclone.validation.ValidPassword;
import com.xclone.validation.ValidationConstants;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

/**
 * Represents a signup request containing user information required to create a new user.
 *
 * @param handle the unique user handle used to identify the account
 * @param password the user's raw password
 * @param displayName the user's displayed name
 * @param bio the user's displayed bio
 * @param profileImage a URI where the user's profile image is stored
 */
public record SignupRequest(
    @Schema(
            requiredMode = Schema.RequiredMode.REQUIRED,
            minLength = 4,
            maxLength = 15,
            pattern = ValidationConstants.HANDLE_PATTERN,
            description =
                "Unique username used to identify the account. Alphanumeric and underscores only, cannot be purely numeric.")
        @ValidHandle
        String handle,
    @Schema(
            requiredMode = Schema.RequiredMode.REQUIRED,
            minLength = 10,
            maxLength = 100,
            pattern = ValidationConstants.PASSWORD_PATTERN,
            description =
                "Account password. Must contain uppercase, lowercase, number, and special character.")
        @ValidPassword
        String password,
    @Schema(description = "Display name shown on the user's profile.", nullable = true)
        @Size(max = 50)
        String displayName,
    @Schema(description = "Short user biography.", nullable = true) @Size(max = 160) String bio,
    @Schema(description = "Profile image URL.", nullable = true) @Size(max = 500)
        String profileImage) {}
