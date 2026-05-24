package com.xclone.share.dto.request;

import com.xclone.validation.ValidationConstants;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

/**
 * Represents the data needed to create a quote. TODO: add mentions
 *
 * @param sharedPostId unique identifier of the shared post
 * @param messageContent text content of the post
 */
public record CreateQuoteInput(
    @NotNull UUID sharedPostId,
    @NotBlank(message = "Post message content is required")
        @Size(max = ValidationConstants.MAX_MESSAGE_CONTENT_SIZE)
        String messageContent) {}
