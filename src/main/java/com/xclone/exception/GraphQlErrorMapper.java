package com.xclone.exception;

import com.xclone.exception.custom.AccountNotActiveException;
import com.xclone.exception.custom.DuplicateFollowException;
import com.xclone.exception.custom.DuplicateHandleException;
import com.xclone.exception.custom.DuplicateRepostException;
import com.xclone.exception.custom.NotPostAuthorException;
import com.xclone.exception.custom.PostNotFoundException;
import com.xclone.exception.custom.SelfFollowException;
import com.xclone.exception.dto.FieldError;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.graphql.GraphQlResponse;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

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
@Slf4j
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

  private static String getFieldName(ConstraintViolation<?> violation) {
    // extract last path segment
    String path = violation.getPropertyPath().toString();
    return path.contains(".") ? path.substring(path.lastIndexOf('.') + 1) : path;
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

  /**
   * Maps a {@link UsernameNotFoundException} to a list of {@link FieldError} DTOs.
   *
   * @param field field responsible for triggering the exception
   * @param ex exception whose message is used as the field-level error message
   * @return a list of {@link FieldError} instances representing the username not found exception
   */
  public static List<FieldError> fromUsernameNotFound(String field, UsernameNotFoundException ex) {
    return List.of(new FieldError(field, ex.getMessage()));
  }

  /**
   * Maps a {@link AccountNotActiveException} to a list of {@link FieldError} DTOs.
   *
   * @param field field responsible for triggering the exception
   * @param ex exception whose message is used as the field-level error message
   * @return a list of {@link FieldError} instances
   */
  public static List<FieldError> fromAccountNotActive(String field, AccountNotActiveException ex) {
    return List.of(new FieldError(field, ex.getMessage()));
  }

  /**
   * Maps a {@link DuplicateFollowException} to a list of {@link FieldError} DTOs.
   *
   * @param ex exception whose message is used as the field-level error message
   * @return a list of {@link FieldError} instances representing the entity violation not found
   *     exception
   */
  public static List<FieldError> fromDuplicateFollow(DuplicateFollowException ex) {
    return List.of(new FieldError("userIdToFollow", ex.getMessage()));
  }

  /**
   * Maps a {@link SelfFollowException} to a list of {@link FieldError} DTOs.
   *
   * @param ex exception whose message is used as the field-level error message
   * @return a list of {@link FieldError} instances representing the entity violation not found
   *     exception
   */
  public static List<FieldError> fromSelfFollow(SelfFollowException ex) {
    return List.of(new FieldError("userIdToFollow", ex.getMessage()));
  }

  /**
   * Maps a {@link NotPostAuthorException} to a list of {@link FieldError} DTOs.
   *
   * @param field field responsible for triggering the exception
   * @param ex exception whose message is used as the field-level error message
   * @return a list of {@link FieldError} instances
   */
  public static List<FieldError> fromNotPostAuthor(String field, NotPostAuthorException ex) {
    return List.of(new FieldError(field, ex.getMessage()));
  }

  /**
   * Maps a {@link PostNotFoundException} to a list of {@link FieldError} DTOs.
   *
   * @param field field responsible for triggering the exception
   * @param ex exception whose message is used as the field-level error message
   * @return a list of {@link FieldError} instances
   */
  public static List<FieldError> fromPostNotFound(String field, PostNotFoundException ex) {
    return List.of(new FieldError(field, ex.getMessage()));
  }

  /**
   * Maps a {@link DuplicateRepostException} to a list of {@link FieldError} DTOs.
   *
   * @param ex exception whose message is used as the field-level error message
   * @return a list of {@link FieldError} instances representing the entity violation not found
   *     exception
   */
  public static List<FieldError> fromDuplicateRepost(DuplicateRepostException ex) {
    log.debug(String.valueOf(ex));
    return List.of(new FieldError("originalPostId", "Repost already exists"));
  }
}
