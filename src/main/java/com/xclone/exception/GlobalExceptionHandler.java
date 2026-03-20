package com.xclone.exception;

import com.xclone.exception.custom.AccountNotActiveException;
import com.xclone.exception.custom.DuplicateHandleException;
import com.xclone.exception.custom.InvalidRefreshTokenException;
import com.xclone.exception.dto.ErrorResponse;
import com.xclone.exception.dto.FieldError;
import com.xclone.exception.dto.ValidationErrorResponse;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Provides global error handling for the REST api routes (auth). There is not a need for a global
 * REST error handler as all the api routes are contained in the same controller, and therefore a
 * local one would suffice. A global REST error handler has been impelmented for learning purposes.
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler
    extends ResponseEntityExceptionHandler { // Is it good to extend this class?

  /**
   * Handles {@link DuplicateHandleException} by returning an HTTP 409 (Conflict) response
   * containing the error message.
   *
   * @param ex the thrown exception
   * @param request the current web request
   * @return a {@link ResponseEntity} containing the error details
   */
  @ExceptionHandler(DuplicateHandleException.class)
  public ResponseEntity<ErrorResponse> handleDuplicateHandleException(
      DuplicateHandleException ex, WebRequest request) {
    log.warn(
        "Duplicate handle attempt: {} - Path: {}", ex.getMessage(), request.getDescription(false));
    return ResponseEntity.status(HttpStatus.CONFLICT.value())
        .body(new ErrorResponse(ex.getMessage()));
  }

  /**
   * Handles {@link InvalidRefreshTokenException} by returning a 401 (Unauthorized) response
   * containing the error message.
   *
   * @param ex the thrown exception
   * @param request the current web request
   * @return a {@link ResponseEntity} containing the error details
   */
  @ExceptionHandler(InvalidRefreshTokenException.class)
  public ResponseEntity<ErrorResponse> handleInvalidRefreshTokenException(
      InvalidRefreshTokenException ex, WebRequest request) {
    log.debug(
        "Invalid refresh token attempt: {} - Path: {}",
        ex.getMessage(),
        request.getDescription(false));
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED.value())
        .body(new ErrorResponse(ex.getMessage()));
  }

  /**
   * Handles {@link AccountNotActiveException} by returning a 403 (Forbidden) response containing
   * the error message.
   *
   * @param ex the thrown exception
   * @param request the current web request
   * @return a {@link ResponseEntity} containing the error details
   */
  @ExceptionHandler(AccountNotActiveException.class)
  public ResponseEntity<ErrorResponse> handleAccountNotActiveException(
      AccountNotActiveException ex, WebRequest request) {
    log.warn(
        "Inactive account attempt: {} - Path: {}", ex.getMessage(), request.getDescription(false));
    return ResponseEntity.status(HttpStatus.FORBIDDEN.value()) // is 410 standard?
        .body(new ErrorResponse(ex.getMessage()));
  }

  /**
   * Handles {@link BadCredentialsException} {@link UsernameNotFoundException} by returning a 401
   * (Unauthorized) response containing the error message.
   *
   * @param ex the thrown exception
   * @param request the current web request
   * @return a {@link ResponseEntity} containing the error details
   */
  @ExceptionHandler({BadCredentialsException.class, UsernameNotFoundException.class})
  public ResponseEntity<ErrorResponse> handleAuthenticationException(
      RuntimeException ex, WebRequest request) {
    log.debug(
        "Authentication failed: {} - Path: {}", ex.getMessage(), request.getDescription(false));
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED.value())
        .body(new ErrorResponse(ex.getMessage()));
  }

  @Override
  public ResponseEntity<Object> handleMethodArgumentNotValid(
      MethodArgumentNotValidException ex,
      @NonNull HttpHeaders headers,
      @NonNull HttpStatusCode status,
      @NonNull WebRequest request) {

    List<FieldError> fieldErrorsList =
        ex.getBindingResult().getFieldErrors().stream()
            .map(error -> new FieldError(error.getField(), error.getDefaultMessage()))
            .toList();

    log.warn("Validation failed: {} errors", fieldErrorsList.size());

    return ResponseEntity.status(HttpStatus.BAD_REQUEST.value())
        .body(new ValidationErrorResponse("Invalid request", fieldErrorsList));
  }

  /**
   * Handles generic exceptions by returning a 500 (Internal Server Error) response containing a
   * generic error message whilst logging the actual error message.
   *
   * @param ex the thrown exception
   * @param request the current web request
   * @return a {@link ResponseEntity} containing the error details
   */
  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleGenericException(Exception ex, WebRequest request) {
    log.error("Unexpected error at {}: ", request.getDescription(false), ex);
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR.value())
        .body(new ErrorResponse("An unexpected error occurred"));
  }
}
