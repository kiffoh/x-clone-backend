package com.xclone.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Composite validation annotation for user handles.
 *
 * <p>Enforces:
 *
 * <ul>
 *   <li>Length between 4 and 15 characters
 *   <li>Alphanumeric characters and underscores only
 *   <li>Cannot consist of digits only
 * </ul>
 */
@Size(min = ValidationConstants.MIN_HANDLE_SIZE, max = ValidationConstants.MAX_HANDLE_SIZE)
@Pattern(
    regexp = ValidationConstants.HANDLE_PATTERN,
    message =
        "Handle may only contain letters, numbers, and underscores, and cannot be all numbers")
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = {})
@Documented
public @interface ValidHandle {

  /**
   * The default validation message.
   *
   * @return the error message template
   */
  String message() default "Invalid handle";

  /**
   * Allows specification of validation groups.
   *
   * @return validation groups
   */
  Class<?>[] groups() default {};

  /**
   * Payload used by clients of the Bean Validation API.
   *
   * @return payload types
   */
  Class<? extends Payload>[] payload() default {};
}
