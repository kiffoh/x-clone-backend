package com.xclone.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import com.xclone.exception.custom.DuplicateHandleException;
import com.xclone.exception.dto.FieldError;
import com.xclone.user.dto.request.UpdateUserInput;
import com.xclone.validation.ValidationConstants;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

public class GraphQlErrorMapperTest {
  @Test
  void mapsDuplicateHandleToFieldError() {
    String exMessage = "Handle is already taken";
    DuplicateHandleException ex = new DuplicateHandleException(exMessage);

    List<FieldError> exceptions = GraphQlErrorMapper.fromDuplicateHandle(ex);
    assertThat(exceptions).hasSize(1);
    assertThat(exceptions.getFirst().field()).isEqualTo("handle");
    assertThat(exceptions.getFirst().message()).isEqualTo(exMessage);
  }

  Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

  @Test
  void mapsSingleConstraintViolationToFieldError() {
    String handleWithSingleViolation = "xxx!";
    UpdateUserInput input = new UpdateUserInput(null, handleWithSingleViolation, null, null);
    Set<ConstraintViolation<UpdateUserInput>> violations = validator.validate(input);
    ConstraintViolationException ex = new ConstraintViolationException(violations);

    List<FieldError> exceptions = GraphQlErrorMapper.fromConstraintViolations(ex);
    assertThat(exceptions).hasSize(1);
    assertThat(exceptions.getFirst().field()).isEqualTo("handle");
    assertThat(exceptions.getFirst().message()).isEqualTo(ValidationConstants.INVALID_HANDLE_REGEX);
  }

  @Test
  void mapsMultipleConstraintViolationToFieldErrors() {
    String handleWithMultipleViolations = "xx!";
    UpdateUserInput input = new UpdateUserInput(null, handleWithMultipleViolations, null, null);
    Set<ConstraintViolation<UpdateUserInput>> violations = validator.validate(input);
    ConstraintViolationException ex = new ConstraintViolationException(violations);

    List<FieldError> exceptions = GraphQlErrorMapper.fromConstraintViolations(ex);
    assertThat(exceptions).hasSize(2);
    assertThat(exceptions)
        .extracting(FieldError::field, FieldError::message)
        .containsExactlyInAnyOrder(
            tuple("handle", ValidationConstants.INVALID_HANDLE_REGEX),
            tuple("handle", ValidationConstants.INVALID_HANDLE_SIZE));
  }
}
