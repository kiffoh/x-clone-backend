package com.xclone.exception.custom;

/** Thrown to indicate that the provided cursor is malformed. */
public class InvalidCursorException extends RuntimeException {
  public InvalidCursorException(String message) {
    super(message);
  }
}
