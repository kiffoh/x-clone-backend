package com.xclone.user.dto.request;

import com.xclone.validation.ObjectNotEmpty;
import com.xclone.validation.ValidHandle;
import jakarta.validation.constraints.Size;

/**
 * Represents an update user request; contains the optional fields a user may update on their
 * profile.
 *
 * <p>Does not contain password as this is updated in the authentication endpoints with. TODO: {
 * updateMyPassword}.
 *
 * @param displayName the user's displayed name
 * @param handle the unique user handle used to identify the account
 * @param bio the user's displayed bio
 * @param profileImage a URI where the user's profile image is stored
 */
@ObjectNotEmpty(message = "UpdateUserInput must have at least one field")
public record UpdateUserInput(
    @Size(max = 50) String displayName,
    @ValidHandle String handle,
    @Size(max = 160) String bio,
    @Size(max = 500) String profileImage) {}
