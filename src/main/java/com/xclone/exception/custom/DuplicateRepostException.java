package com.xclone.exception.custom;

/**
 * Thrown to indicate that a user attempted to repost a post which is already reposted by that user.
 */
public class DuplicateRepostException extends RuntimeException {
  public DuplicateRepostException(String message) {
    super(message);
  }
}
