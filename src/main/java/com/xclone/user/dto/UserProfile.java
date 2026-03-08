package com.xclone.user.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Immutable public-facing projection of a {@link com.xclone.user.model.entity.User} entity, mapping
 * to the {@code User} type in the GraphQL schema.
 *
 * @param id UUID of the user
 * @param handle the unique user handle used to identify the account
 * @param displayName the user's displayed name
 * @param bio the user's displayed bio
 * @param profileImage a URI where the user's profile image is stored
 * @param createdAt datetime of user entity creation
 * @param updatedAt datetime of last update of user entity
 */
public record UserProfile(
    UUID id,
    String handle,
    String displayName,
    String bio,
    String profileImage,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt) {}
