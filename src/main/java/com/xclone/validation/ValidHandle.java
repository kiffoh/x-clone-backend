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

@NotBlank(message = "Handle is required")
@Size(min = 1, max = 50)
@Pattern(regexp = "^[0-9a-zA-Z_]+$")
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = {})
@Documented
public @interface ValidHandle {
  String message() default "Invalid handle";

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};
}
