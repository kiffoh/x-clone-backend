package com.xclone.exception.custom;

/**
 * Thrown when an account is suspended or deleted, and therefore not elibigle to perform the request
 * operation.
 */
public class AccountNotActiveException extends RuntimeException {
  public AccountNotActiveException(String message) {
    super(message);
  }
}
