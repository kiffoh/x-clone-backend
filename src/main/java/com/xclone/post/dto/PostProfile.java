package com.xclone.post.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Immutable public-facing projection of a {@link com.xclone.post.model.entity.Post} entity, mapping
 * to the {@code Post} type in the GraphQL schema.
 *
 * <p>messageContent is nullable for pure reposts
 *
 * @param id UUID of the post
 * @param authorId UUID of the author
 * @param messageContent optional text content of a post
 * @param createdAt datetime of post entity creation
 * @param updatedAt datetime of last update of post entity
 */
public record PostProfile(
    UUID id,
    UUID authorId,
    String messageContent,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt) {}
