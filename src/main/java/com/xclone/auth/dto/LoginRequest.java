package com.xclone.auth.dto;

import com.xclone.validation.ValidHandle;
import com.xclone.validation.ValidPassword;
import com.xclone.validation.ValidationConstants;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Represents a login request containing user credentials required for authentication.
 *
 * @param handle   the unique user handle used to identify the account
 * @param password the user's raw password
 */
public record LoginRequest(
    @Schema(
        requiredMode = Schema.RequiredMode.REQUIRED,
        minLength = 4,
        maxLength = 15,
        pattern = ValidationConstants.HANDLE_PATTERN,
        description =
            "Unique username used to identify the account. "
                + "Alphanumeric and underscores only, cannot be purely numeric.")
    @ValidHandle
    String handle,
    @Schema(
        requiredMode = Schema.RequiredMode.REQUIRED,
        minLength = 10,
        maxLength = 100,
        pattern = ValidationConstants.PASSWORD_PATTERN,
        description =
            "Account password. "
                + "Must contain uppercase, lowercase, number, and special character.")
    @ValidPassword
    String password) {
}
