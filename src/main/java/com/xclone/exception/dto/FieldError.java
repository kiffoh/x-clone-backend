package com.xclone.exception.dto;

/**
 * Represents a single field-level validation error.
 *
 * @param field the name of the field that failed validation
 * @param message the associated validation error message
 */
public record FieldError(String field, String message) {}
