package com.xclone.exception;

import com.xclone.exception.custom.DuplicateHandleException;
import com.xclone.exception.dto.FieldError;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.util.List;

public class GraphQlErrorMapper {

  public static List<FieldError> fromConstraintViolations(ConstraintViolationException ex) {
    return ex.getConstraintViolations().stream()
        .map(v -> new FieldError(getFieldName(v), v.getMessage()))
        .toList();
  }

  public static List<FieldError> fromDuplicateHandle(DuplicateHandleException ex) {
    return List.of(new FieldError("handle", ex.getMessage()));
  }

  private static String getFieldName(ConstraintViolation<?> violation) {
    // extract last path segment
    String path = violation.getPropertyPath().toString();
    return path.contains(".") ? path.substring(path.lastIndexOf('.') + 1) : path;
  }
}
