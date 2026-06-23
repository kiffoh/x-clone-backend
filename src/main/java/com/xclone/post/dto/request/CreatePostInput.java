package com.xclone.post.dto.request;

import com.xclone.validation.ValidationConstants;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

/**
 * Represents the data needed to create a pure post.
 *
 * @param messageContent text content of the post
 * @param mentionedUserIds optional list of unique identifiers of users which are mentioned in the
 *     {@code messageContent}
 */
public record CreatePostInput(
    @NotBlank(message = "Post message content is required")
        @Size(max = ValidationConstants.MAX_MESSAGE_CONTENT_SIZE)
        String messageContent,
    List<UUID> mentionedUserIds) {}
