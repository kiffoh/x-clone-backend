package com.xclone.exception.custom;

/** Thrown when a post with the queried id cannot be found in the database. */
public class PostNotFoundException extends RuntimeException {
  public PostNotFoundException(String message) {
    super(message);
  }
}
