package com.xclone.exception.dto;

/**
 * Represents a generic error response returned to the client.
 *
 * @param message a human-readable error message
 */
public record ErrorResponse(String message) {}
