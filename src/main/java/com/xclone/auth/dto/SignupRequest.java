package com.xclone.auth.dto;

import com.xclone.validation.ValidHandle;
import com.xclone.validation.ValidPassword;
import com.xclone.validation.ValidationConstants;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
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
            minLength = ValidationConstants.MIN_HANDLE_SIZE,
            maxLength = ValidationConstants.MAX_HANDLE_SIZE,
            pattern = ValidationConstants.HANDLE_PATTERN,
            description =
                "Unique username used to identify the account. "
                    + "Alphanumeric and underscores only, cannot be purely numeric.")
        @NotBlank(message = "Handle is required")
        @ValidHandle
        String handle,
    @Schema(
            requiredMode = Schema.RequiredMode.REQUIRED,
            minLength = ValidationConstants.MIN_PASSWORD_SIZE,
            maxLength = ValidationConstants.MAX_PASSWORD_SIZE,
            pattern = ValidationConstants.PASSWORD_PATTERN,
            description =
                "Account password. "
                    + "Must contain uppercase, lowercase, number, and special character.")
        @ValidPassword
        String password,
    @Schema(description = "Display name shown on the user's profile.", nullable = true)
        @Size(max = ValidationConstants.MAX_DISPLAY_NAME_SIZE)
        String displayName,
    @Schema(description = "Short user biography.", nullable = true)
        @Size(max = ValidationConstants.MAX_BIO_SIZE)
        String bio,
    @Schema(description = "Profile image URL.", nullable = true)
        @Size(max = ValidationConstants.MAX_PROFILE_IMAGE_SIZE)
        String profileImage) {}
