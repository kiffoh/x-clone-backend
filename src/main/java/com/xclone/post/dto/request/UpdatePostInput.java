package com.xclone.post.dto.request;

import com.xclone.validation.ValidationConstants;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

/**
 * Represents an update post request.
 *
 * @param postId unique identifier of the post to be updated
 * @param messageContent the message content of the post
 * @param mentionedUserIds optional list of unique identifiers of users which are mentioned in the
 *     {@code messageContent}
 */
public record UpdatePostInput(
    @NotNull UUID postId,
    @NotBlank(message = "Post message content is required")
        @Size(max = ValidationConstants.MAX_MESSAGE_CONTENT_SIZE)
        String messageContent,
    List<UUID> mentionedUserIds) {}
