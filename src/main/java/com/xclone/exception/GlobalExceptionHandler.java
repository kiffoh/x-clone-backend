package com.xclone.exception;

import com.xclone.exception.custom.AccountNotActiveException;
import com.xclone.exception.custom.DuplicateHandleException;
import com.xclone.exception.custom.InvalidRefreshTokenException;
import com.xclone.exception.dto.ErrorResponse;
import com.xclone.exception.dto.FieldError;
import com.xclone.exception.dto.ValidationErrorResponse;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Provides global error handling for the REST api routes (auth).
 * There is not a need for a global REST error handler as all the api routes are contained in
 * the same controller, and therefore a local one would suffice.
 * A global REST error handler has been impelmented for learning purposes.
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler
    extends ResponseEntityExceptionHandler { // Is it good to extend this class?

  @ExceptionHandler(DuplicateHandleException.class)
  public ResponseEntity<ErrorResponse> handleDuplicateHandleException(
      DuplicateHandleException ex, WebRequest request) {
    log.warn("Duplicate handle attempt: {} - Path: {}",
        ex.getMessage(),
        request.getDescription(false));
    return ResponseEntity.status(HttpStatus.CONFLICT.value())
        .body(new ErrorResponse(ex.getMessage()));
  }

  @ExceptionHandler(InvalidRefreshTokenException.class)
  public ResponseEntity<ErrorResponse> handleInvalidRefreshTokenException(
      InvalidRefreshTokenException ex, WebRequest request) {
    log.warn("Invalid refresh token attempt: {} - Path: {}",
        ex.getMessage(),
        request.getDescription(false));
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED.value())
        .body(new ErrorResponse(ex.getMessage()));
  }

  @ExceptionHandler(AccountNotActiveException.class)
  public ResponseEntity<ErrorResponse> handleAccountNotActiveException(
      AccountNotActiveException ex, WebRequest request) {
    log.warn("Inactive account attempt: {} - Path: {}",
        ex.getMessage(),
        request.getDescription(false));
    return ResponseEntity.status(HttpStatus.FORBIDDEN.value()) // is 410 standard?
        .body(new ErrorResponse(ex.getMessage()));
  }

  @ExceptionHandler(BadCredentialsException.class)
  public ResponseEntity<ErrorResponse> handleBadCredentialsException(
      BadCredentialsException ex, WebRequest request) {
    log.warn("Authentication failed: {} - Path: {}",
        ex.getMessage(),
        request.getDescription(false));
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED.value()) // is 410 standard?
        .body(new ErrorResponse(ex.getMessage()));
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ValidationErrorResponse> handleMethodArgumentNotValidException(
      MethodArgumentNotValidException ex) {

    List<FieldError> fieldErrorsList = ex.getBindingResult().getFieldErrors().stream()
        .map(error -> new FieldError(error.getField(), error.getDefaultMessage())).toList();

    log.warn("Validation failed: {} errors", fieldErrorsList.size());

    return ResponseEntity.status(HttpStatus.BAD_REQUEST.value())
        .body(new ValidationErrorResponse("Invalid request", fieldErrorsList));
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleGenericException(
      Exception ex, WebRequest request) {
    log.error("Unexpected error at {}: ",
        request.getDescription(false), ex);
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR.value())
        .body(new ErrorResponse(ex.getMessage()));
  }
}
