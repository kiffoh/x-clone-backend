package com.xclone.validation;

/** Acts as a single source of truth for validation constants to prevent variable drift. */
public final class ValidationConstants {
  public static final String HANDLE_PATTERN = "^(?![0-9]+$)[0-9a-zA-Z_]+$";
  public static final int MIN_HANDLE_SIZE = 4;
  public static final int MAX_HANDLE_SIZE = 15;
  public static final String INVALID_HANDLE_SIZE =
      String.format("size must be between %d and %d", MIN_HANDLE_SIZE, MAX_HANDLE_SIZE);
  public static final String INVALID_HANDLE_REGEX =
      "Handle may only contain letters, numbers, and underscores, and cannot be all numbers";

  public static final String PASSWORD_PATTERN =
      "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[$&+,:;=?@#|'<>.^*()%!-])"
          + "[0-9a-zA-Z$&+,:;=?@#|'<>.^*()%!-]+$";
  public static final int MIN_PASSWORD_SIZE = 10;
  public static final int MAX_PASSWORD_SIZE = 100;
  public static final String INVALID_PASSWORD_SIZE =
      String.format("size must be between %d and %d", MIN_PASSWORD_SIZE, MAX_PASSWORD_SIZE);
  public static final String INVALID_PASSWORD_REGEX =
      "Password must contain at least one special character,"
          + " capital letter, lowercase letter and number";
}
