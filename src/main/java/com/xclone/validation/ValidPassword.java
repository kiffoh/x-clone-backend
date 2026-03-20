package com.xclone.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Composite validation annotation for user passwords.
 *
 * <p>Enforces:
 *
 * <ul>
 *   <li>Non-blank value
 *   <li>Length between 10 and 100 characters
 *   <li>Alphanumeric and special characters only
 *   <li>Must have at least one capital letter, one lower case letter, and one number
 * </ul>
 */
@NotBlank(message = "Password is required")
@Size(min = ValidationConstants.MIN_PASSWORD_SIZE, max = ValidationConstants.MAX_PASSWORD_SIZE)
@Pattern(
    regexp = ValidationConstants.PASSWORD_PATTERN,
    message =
        "Password must contain at least one special character, "
            + "capital letter, lowercase letter and number")
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = {})
@Documented
public @interface ValidPassword {
  /**
   * The default validation message.
   *
   * @return the error message template
   */
  String message() default "Invalid password";

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
