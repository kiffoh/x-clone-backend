package com.xclone.exception;

import com.xclone.exception.custom.DuplicateHandleException;
import com.xclone.exception.dto.FieldError;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.util.List;
import org.springframework.graphql.GraphQlResponse;

/**
 * Maps domain and validation exceptions to {@link FieldError} DTOs for GraphQL responses.
 *
 * <p>This mapper supports the "errors as data" pattern by translating backend exceptions (e.g.
 * constraint violations or business rule conflicts) into structured field-level errors that are
 * returned within the {@code data.errors} field of a {@link GraphQlResponse}, rather than being
 * surfaced via top-level GraphQL errors.
 *
 * <p>Each mapping method handles a specific exception type and produces one or more {@link
 * FieldError} instances containing a field name and user-facing message.
 */
public class GraphQlErrorMapper {

  /**
   * Maps a {@link ConstraintViolationException} to a list of {@link FieldError} DTOs.
   *
   * <p>Each {@link ConstraintViolation} is converted into a field-level error by extracting the
   * leaf property name from the violation path and pairing it with the associated validation
   * message.
   *
   * @param ex the exception containing one or more constraint violations
   * @return a list of {@link FieldError} instances representing each violation
   */
  public static List<FieldError> fromConstraintViolations(ConstraintViolationException ex) {
    return ex.getConstraintViolations().stream()
        .map(v -> new FieldError(getFieldName(v), v.getMessage()))
        .toList();
  }

  /**
   * Maps a {@link DuplicateHandleException} to a list of {@link FieldError} DTOs.
   *
   * @param ex exception whose message is used as the field-level error message
   * @return a list of {@link FieldError} instances representing the duplicate handle exception
   */
  public static List<FieldError> fromDuplicateHandle(DuplicateHandleException ex) {
    return List.of(new FieldError("handle", ex.getMessage()));
  }

  private static String getFieldName(ConstraintViolation<?> violation) {
    // extract last path segment
    String path = violation.getPropertyPath().toString();
    return path.contains(".") ? path.substring(path.lastIndexOf('.') + 1) : path;
  }
}
