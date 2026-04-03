package com.xclone.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import com.xclone.user.dto.request.UpdateUserInput;
import com.xclone.validation.ValidationConstants;
import graphql.GraphQLError;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;

public class GlobalGraphQlExceptionHandlerTest {
  GlobalGraphQlExceptionHandler graphQlExceptionHandler = new GlobalGraphQlExceptionHandler();
  Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

  @Test
  void mapsSingleConstraintViolationToGraphQlErrorList() {
    String handleWithSingleViolation = "xxx!";
    UpdateUserInput input = new UpdateUserInput(null, handleWithSingleViolation, null, null);
    Set<ConstraintViolation<UpdateUserInput>> violations = validator.validate(input);
    ConstraintViolationException formattedExceptions = new ConstraintViolationException(violations);

    List<GraphQLError> exceptions =
        graphQlExceptionHandler.handleConstraintViolationException(formattedExceptions);
    assertThat(exceptions).hasSize(1);
    assertThat(exceptions)
        .extracting(
            GraphQLError::getMessage, GraphQLError::getErrorType, GraphQLError::getExtensions)
        .containsExactlyInAnyOrder(
            tuple(
                ValidationConstants.INVALID_HANDLE_REGEX,
                ErrorType.BAD_REQUEST,
                Map.of("field", "handle")));
  }

  @Test
  void mapsMultipleConstraintViolationToGraphQlErrorList() {
    String handleWithMultipleViolations = "xx!";
    UpdateUserInput input = new UpdateUserInput(null, handleWithMultipleViolations, null, null);
    Set<ConstraintViolation<UpdateUserInput>> violations = validator.validate(input);
    ConstraintViolationException formattedExceptions = new ConstraintViolationException(violations);

    List<GraphQLError> exceptions =
        graphQlExceptionHandler.handleConstraintViolationException(formattedExceptions);
    assertThat(exceptions).hasSize(2);
    assertThat(exceptions)
        .extracting(
            GraphQLError::getMessage, GraphQLError::getErrorType, GraphQLError::getExtensions)
        .containsExactlyInAnyOrder(
            tuple(
                ValidationConstants.INVALID_HANDLE_REGEX,
                ErrorType.BAD_REQUEST,
                Map.of("field", "handle")),
            tuple(
                ValidationConstants.INVALID_HANDLE_SIZE,
                ErrorType.BAD_REQUEST,
                Map.of("field", "handle")));
  }

  @Test
  void handlesBindException() {
    FieldError fieldError =
        new FieldError(
            "object", "field", "not a valid UUID", false, null, null, "not a valid UUID");
    BindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "object");
    bindingResult.addError(fieldError);
    BindException ex = new BindException(bindingResult);

    GraphQLError formattedExceptions = graphQlExceptionHandler.handleBindException(ex);

    assertThat(formattedExceptions.getMessage())
        .isEqualTo("Invalid value 'not a valid UUID' for argument");
    assertThat(formattedExceptions.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
  }

  @Test
  void handlesGenericException() {
    Exception ex = new RuntimeException("Unexpected error");
    GraphQLError formattedExceptions = graphQlExceptionHandler.handleGenericException(ex);

    assertThat(formattedExceptions.getMessage()).isEqualTo("An unexpected error occurred");
    assertThat(formattedExceptions.getErrorType()).isEqualTo(ErrorType.INTERNAL_ERROR);
  }
}
