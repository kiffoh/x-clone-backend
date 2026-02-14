package com.xclone.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.xclone.exception.custom.AccountNotActiveException;
import com.xclone.exception.custom.DuplicateHandleException;
import com.xclone.exception.custom.InvalidRefreshTokenException;
import com.xclone.exception.dto.ErrorResponse;
import com.xclone.exception.dto.FieldError;
import com.xclone.exception.dto.ValidationErrorResponse;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.context.request.WebRequest;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

  private GlobalExceptionHandler handler;
  private final String PATH_TO_ERROR = "path to error";

  @Mock
  private WebRequest request;

  @BeforeEach
  void setUp() {
    handler = new GlobalExceptionHandler();
    lenient().when(request.getDescription(false)).thenReturn(PATH_TO_ERROR);
  }

  @Test
  void handleDuplicateHandleException_returns409() {
    DuplicateHandleException ex = new DuplicateHandleException("This handle already exists");
    ResponseEntity<ErrorResponse> response = handler.handleDuplicateHandleException(ex, request);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().message()).isEqualTo("This handle already exists");
  }

  @Test
  void handleInvalidRefreshTokenException_returns401() {
    InvalidRefreshTokenException ex = new InvalidRefreshTokenException("Token's don't match");
    ResponseEntity<ErrorResponse> response =
        handler.handleInvalidRefreshTokenException(ex, request);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().message()).isEqualTo("Token's don't match");
  }

  @Test
  void handleAccountNotActiveException_returns403() {
    AccountNotActiveException ex = new AccountNotActiveException("Account is not active");
    ResponseEntity<ErrorResponse> response =
        handler.handleAccountNotActiveException(ex, request);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().message()).isEqualTo("Account is not active");
  }

  @Test
  void handleBadCredentialsException_returns401() {
    BadCredentialsException ex = new BadCredentialsException("Authentication failed");
    ResponseEntity<ErrorResponse> response = handler.handleBadCredentialsException(ex, request);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().message()).isEqualTo("Authentication failed");
  }

  @Test
  void handleMethodArgumentNotValidException_returns400WithFieldErrors() {
    // Setup
    BeanPropertyBindingResult bindingResult =
        new BeanPropertyBindingResult(new Object(), "signupRequest");
    bindingResult.addError(new org.springframework.validation.FieldError(
        "signupRequest",
        "handle",
        "must not be blank"
    ));

    bindingResult.addError(new org.springframework.validation.FieldError(
        "signupRequest",
        "password",
        "size must be between 8 and 100"
    ));

    MethodParameter parameter = mock(MethodParameter.class);
    MethodArgumentNotValidException ex =
        new MethodArgumentNotValidException(parameter, bindingResult);

    // Act
    HttpHeaders headers = HttpHeaders.EMPTY;
    HttpStatusCode status = HttpStatus.BAD_REQUEST;
    ResponseEntity<Object> response =
        handler.handleMethodArgumentNotValid(ex, headers, status, request);

    // Asserts
    assertThat(response).isNotNull();
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody()).isNotNull();
    ValidationErrorResponse body = (ValidationErrorResponse) response.getBody();

    assertThat(body.message()).isEqualTo("Invalid request");
    assertThat(body.errors().size()).isEqualTo(2);
    assertThat(body.errors()).extracting(
        FieldError::field,
        FieldError::message
    ).containsExactlyInAnyOrder(
        tuple("handle", "must not be blank"),
        tuple("password", "size must be between 8 and 100")
    );
  }

  @Test
  void handleGenericException_returns500() {
    // Setup log capture
    Logger logger = (Logger) LoggerFactory.getLogger(GlobalExceptionHandler.class);
    ListAppender<ILoggingEvent> logWatcher = new ListAppender<>();
    logWatcher.start();
    logger.addAppender(logWatcher);

    Exception ex = new Exception("Internal Server Error");

    List<ILoggingEvent> logsList = logWatcher.list;

    ResponseEntity<ErrorResponse> response = handler.handleGenericException(ex, request);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().message()).isEqualTo("Internal Server Error");
    assertThat(logsList.size()).isEqualTo(1);
    assertThat(logsList.getFirst().getLevel()).isEqualTo(Level.ERROR);
    assertThat(logsList.getFirst().getFormattedMessage())
        .contains(String.format("Unexpected error at %s", PATH_TO_ERROR));
  }


}