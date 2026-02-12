package com.xclone.exception.dto;

import java.util.List;

public record ValidationErrorResponse(String message, List<FieldError> errors) {
}
