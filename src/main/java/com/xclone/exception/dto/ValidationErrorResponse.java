package com.xclone.exception.dto;

import java.util.List;

/**
 * Response body returned when request validation fails.
 *
 * @param message summary validation message
 * @param errors list of field-level validation errors
 */
public record ValidationErrorResponse(String message, List<FieldError> errors) {}
