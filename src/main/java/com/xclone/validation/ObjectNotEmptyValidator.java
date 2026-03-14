package com.xclone.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.lang.reflect.Field;

public class ObjectNotEmptyValidator implements ConstraintValidator<ObjectNotEmpty, Object> {
  @Override
  public boolean isValid(Object o, ConstraintValidatorContext constraintValidatorContext) {
    boolean fieldExists = false;
    for (Field field : o.getClass().getDeclaredFields()) {
      field.setAccessible(true);
      try {
        if (field.get(o) != null) {
          fieldExists = true;
          break;
        }
      } catch (IllegalAccessException ex) {
        throw new RuntimeException(ex);
      }
    }
    return fieldExists;
  }
}
