package com.xclone.exception.custom;

/** Thrown to indicate that a user attempted to register with a handle that is already in use. */
public class DuplicateHandleException extends RuntimeException {
  public DuplicateHandleException(String message) {
    super(message);
  }
}
