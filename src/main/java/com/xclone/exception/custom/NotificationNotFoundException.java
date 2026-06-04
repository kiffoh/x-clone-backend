package com.xclone.exception.custom;

/** Thrown when a notification with the queried id cannot be found in the database. */
public class NotificationNotFoundException extends RuntimeException {
  public NotificationNotFoundException(String message) {
    super(message);
  }
}
