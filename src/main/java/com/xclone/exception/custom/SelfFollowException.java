package com.xclone.exception.custom;

/** Thrown to indicate that a user attempted to its own account. */
public class SelfFollowException extends RuntimeException {
  public SelfFollowException(String message) {
    super(message);
  }
}
