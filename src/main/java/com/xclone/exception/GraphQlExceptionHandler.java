package com.xclone.exception;

import graphql.ErrorType;
import graphql.GraphQLError;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ControllerAdvice;

@ControllerAdvice
@Slf4j
public class GraphQlExceptionHandler {

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
}
