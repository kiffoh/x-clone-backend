package com.xclone.exception.custom;

/**
 * Thrown to indicate that a provided refresh token is invalid, expired, or otherwise cannot be
 * trusted.
 */
public class InvalidRefreshTokenException extends RuntimeException {
  public InvalidRefreshTokenException(String message) {
    super(message);
  }
}
