package com.xclone.auth.dto;

import com.xclone.validation.ValidHandle;
import com.xclone.validation.ValidPassword;
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
    @ValidHandle String handle,
    @ValidPassword String password,
    @Size(max = 50) String displayName,
    @Size(max = 160) String bio,
    @Size(max = 500) String profileImage) {}
