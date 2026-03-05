package com.xclone.validation;

public final class ValidationConstants {
  public static final String HANDLE_PATTERN = "^(?![0-9]+$)[0-9a-zA-Z_]+$";
  public static final String PASSWORD_PATTERN =
      "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[$&+,:;=?@#|'<>.^*()%!-])"
          + "[0-9a-zA-Z$&+,:;=?@#|'<>.^*()%!-]+$";
}
