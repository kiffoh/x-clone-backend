package com.xclone.exception.custom;

/** Thrown to indicate that a user attempted to follow account which is already follows. */
public class DuplicateFollowException extends RuntimeException {
  public DuplicateFollowException(String message) {
    super(message);
  }
}
