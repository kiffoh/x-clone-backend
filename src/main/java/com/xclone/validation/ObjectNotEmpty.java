package com.xclone.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Composite validation annotation for DTOs which must contain at least one value. */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = {ObjectNotEmptyValidator.class})
@Documented
public @interface ObjectNotEmpty {
  /**
   * The default validation message.
   *
   * @return the error message template
   */
  String message() default "Invalid object";

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
