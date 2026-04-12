package com.xclone.post.dto.request;

import com.xclone.validation.ValidationConstants;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.UUID;

/**
 * Represents an update post request.
 *
 * @param id unique identifier of the post to be updated
 * @param messageContent the message content of the post
 */
public record UpdatePostInput(
    @NotBlank UUID id,
    @NotBlank(message = "Post message content is required")
        @Size(max = ValidationConstants.MAX_MESSAGE_CONTENT_SIZE)
        String messageContent) {}
