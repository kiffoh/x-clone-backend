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
 * Composite validation annotation for user handles.
 *
 * <p>Enforces:
 *
 * <ul>
 *   <li>Non-blank value
 *   <li>Length between 4 and 15 characters
 *   <li>Alphanumeric characters and underscores only
 *   <li>Cannot consist of digits only
 * </ul>
 */
@NotBlank(message = "Handle is required")
@Size(min = 4, max = 15)
@Pattern(regexp = "^(?![0-9]+$)[0-9a-zA-Z_]+$")
@Target({ElementType.FIELD})
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
