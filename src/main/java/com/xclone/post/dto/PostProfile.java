package com.xclone.post.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Immutable public-facing projection of a {@link com.xclone.post.model.entity.Post} entity, mapping
 * to the {@code Post} type in the GraphQL schema.
 *
 * <p>messageContent is nullable for pure reposts
 *
 * @param id unique identifier of the post
 * @param authorId unique identifier of the author
 * @param parentId optional unique identifier of the parent post
 * @param sharedPostId optional unique identifier of the shared post
 * @param messageContent optional text content of a post
 * @param createdAt datetime of post entity creation
 * @param updatedAt datetime of last update of post entity
 */
public record PostProfile(
    UUID id,
    UUID authorId,
    String messageContent,
    UUID parentId,
    UUID sharedPostId,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt) {}
