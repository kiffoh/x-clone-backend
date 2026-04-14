package com.xclone.post.dto.request;

import com.xclone.validation.ValidationConstants;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Represents the data needed to create a pure post. TODO: Remove ObjectNotEmpty when mentions are
 * added
 *
 * @param messageContent text content of the post
 */
public record CreatePostInput(
    @NotBlank(message = "Post message content is required")
        @Size(max = ValidationConstants.MAX_MESSAGE_CONTENT_SIZE)
        String messageContent) {}
