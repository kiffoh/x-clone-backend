package com.xclone.exception.custom;

/**
 * Thrown to indicate that a user is trying to mutate a notification read status for which they are
 * not the recipient of.
 */
public class NotNotificationRecipientException extends RuntimeException {
  public NotNotificationRecipientException(String message) {
    super(message);
  }
}
