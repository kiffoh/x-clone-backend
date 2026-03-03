package com.xclone.auth.dto;

import com.xclone.validation.ValidHandle;
import com.xclone.validation.ValidPassword;

/**
 * Represents a login request containing user credentials required for authentication.
 *
 * @param handle the unique user handle used to identify the account
 * @param password the user's raw password
 */
public record LoginRequest(@ValidHandle String handle, @ValidPassword String password) {}
