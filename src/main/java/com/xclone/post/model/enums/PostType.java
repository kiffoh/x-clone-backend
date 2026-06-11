package com.xclone.post.model.enums;

import com.xclone.notification.model.enums.NotificationType;

/** Enum for each type of post. */
public enum PostType {
  REPOST,
  QUOTE,
  REPLY,
  POST;

  /**
   * Mapping method to convert the {@link PostType} to its corresponding {@link NotificationType}.
   *
   * <p>{@link PostType} contains a subset of {@link NotificationType} values.
   *
   * @return corresponding notification type
   */
  public NotificationType toNotificationType() {
    return switch (this) {
      case REPLY -> NotificationType.REPLY;
      case REPOST -> NotificationType.REPOST;
      case QUOTE -> NotificationType.QUOTE;
      case POST -> throw new IllegalStateException("POST has no corresponding notification type");
    };
  }
}
