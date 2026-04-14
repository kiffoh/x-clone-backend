package com.xclone.exception.custom;

/** Thrown to indicate that a user is trying to mutate a post which they are not the author of. */
public class NotPostAuthorException extends RuntimeException {
  public NotPostAuthorException(String message) {
    super(message);
  }
}
