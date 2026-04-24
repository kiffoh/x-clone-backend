package com.xclone.exception;

import com.xclone.exception.custom.InvalidCursorException;
import com.xclone.exception.custom.NotPostAuthorException;
import com.xclone.exception.dto.FieldError;
import graphql.GraphQLError;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.graphql.data.method.annotation.GraphQlExceptionHandler;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.validation.BindException;
import org.springframework.web.bind.annotation.ControllerAdvice;

/** Provides global error handling for GraphQL operations. */
@ControllerAdvice
@Slf4j
public class GlobalGraphQlExceptionHandler {

  /**
   * Handles {@link ConstraintViolationException} and formats the errors to a similar style to the
   * REST error response* {@link FieldError}.
   *
   * @param violations exception which contains the validation violations
   * @return a list of {@link GraphQLError} — one entry per violation, each containing the violation
   *     message and field name in {@code extensions}.
   */
  @GraphQlExceptionHandler(ConstraintViolationException.class)
  public List<GraphQLError> handleConstraintViolationException(
      ConstraintViolationException violations) {
    List<GraphQLError> fieldErrors =
        violations.getConstraintViolations().stream().map(this::formatValidationError).toList();

    log.warn("Validation failed: {} errors", fieldErrors.size());

    return fieldErrors;
  }

  private GraphQLError formatValidationError(ConstraintViolation<?> violation) {

    String field = getFieldName(violation);

    return GraphQLError.newError()
        .message(violation.getMessage())
        .errorType(ErrorType.BAD_REQUEST)
        .extensions(Map.of("field", field))
        .build();
  }

  private String getFieldName(ConstraintViolation<?> violation) {
    String field = null;
    for (var node : violation.getPropertyPath()) {
      field = node.getName();
    }
    return field;
  }

  /**
   * Handles invalid JSON in requests by returning a {@link BindException} protocol error, logging
   * the exception class, message, and full stack trace.
   *
   * <p>If a field error is present, it is returned as part of the error message.
   *
   * @param ex the unhandled exception
   * @return a GraphQL protocol error with a client-safe message
   */
  @GraphQlExceptionHandler(BindException.class)
  public GraphQLError handleBindException(BindException ex) {
    log.error("Invalid request: {} - {} ", ex.getClass().getSimpleName(), ex.getMessage(), ex);

    String message = "Invalid input format";
    if (ex.getFieldError() != null) {
      message =
          String.format("Invalid value '%s' for argument", ex.getFieldError().getRejectedValue());
    }

    return GraphQLError.newError().message(message).errorType(ErrorType.BAD_REQUEST).build();
  }

  /**
   * Handles unexpected exceptions by returning a generic {@link ErrorType#INTERNAL_ERROR} protocol
   * error, and logs the exception class, message, and full stack trace.
   *
   * <p>The response message is intentionally generic — the actual exception detail is never sent to
   * the client.
   *
   * @param ex the unhandled exception
   * @return a GraphQL protocol error with a safe generic message
   */
  @GraphQlExceptionHandler(Exception.class)
  public GraphQLError handleGenericException(Exception ex) {
    log.error("Unexpected error: {} - {} ", ex.getClass().getSimpleName(), ex.getMessage(), ex);

    return GraphQLError.newError()
        .message("An unexpected error occurred")
        .errorType(ErrorType.INTERNAL_ERROR)
        .build();
  }

  /**
   * Handles an invalid cursor by returning an {@link InvalidCursorException} protocol error,
   * logging the exception class and message.
   *
   * @param ex the unhandled exception
   * @return a GraphQL protocol error with a client-safe message
   */
  @GraphQlExceptionHandler(InvalidCursorException.class)
  public GraphQLError handleInvalidCursor(InvalidCursorException ex) {
    log.warn("Invalid request: {} - {} ", ex.getClass().getSimpleName(), ex.getMessage());

    return GraphQLError.newError()
        .message(ex.getMessage()) // e.g., "The provided cursor is malformed"
        .errorType(ErrorType.BAD_REQUEST)
        .build();
  }

  /**
   * Handles a forbidden request by returning an {@link NotPostAuthorException} protocol error,
   * logging the exception class and message.
   *
   * @param ex the unhandled exception
   * @return a GraphQL protocol error with a client-safe message
   */
  @GraphQlExceptionHandler(NotPostAuthorException.class)
  public GraphQLError handleNotPostAuthor(NotPostAuthorException ex) {
    log.warn("Forbidden request: {} - {} ", ex.getClass().getSimpleName(), ex.getMessage());

    return GraphQLError.newError()
        .message("Not post author")
        .errorType(ErrorType.FORBIDDEN)
        .build();
  }
}
