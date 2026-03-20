package com.xclone.exception;

import com.xclone.exception.dto.FieldError;
import graphql.ErrorType;
import graphql.GraphQLError;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ControllerAdvice;

/** Provides global error handling for GraphQL operations. */
@ControllerAdvice
@Slf4j
public class GraphQlExceptionHandler {

  /**
   * Handles {@link ConstraintViolationException} and formats the errors to a similar style to the
   * REST error response* {@link FieldError}.
   *
   * @param violations exception which contains the validation violations
   * @return a list of {@link GraphQLError} — one entry per violation, each containing the violation
   *     message and field name in {@code extensions}.
   */
  @org.springframework.graphql.data.method.annotation.GraphQlExceptionHandler(
      ConstraintViolationException.class)
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
        .errorType(ErrorType.ValidationError)
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
   * Handles unexpected exceptions by returning a generic {@link ErrorType#DataFetchingException}
   * protocol error, and logs the exception class, message, and full stack trace.
   *
   * <p>The response message is intentionally generic — the actual exception detail is never sent to
   * the client.
   *
   * @param ex the unhandled exception
   * @return a GraphQL protocol error with a safe generic message
   */
  @org.springframework.graphql.data.method.annotation.GraphQlExceptionHandler(Exception.class)
  public GraphQLError handleGenericException(Exception ex) {
    log.error("Unexpected error: {} - {} ", ex.getClass().getSimpleName(), ex.getMessage(), ex);

    return GraphQLError.newError()
        .message("An unexpected error occurred")
        .errorType(ErrorType.DataFetchingException)
        .build();
  }
}
