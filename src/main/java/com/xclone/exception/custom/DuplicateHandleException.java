package com.xclone.exception.custom;

public class DuplicateHandleException extends RuntimeException {
  public DuplicateHandleException(String message) {
    super(message);
  }
}
