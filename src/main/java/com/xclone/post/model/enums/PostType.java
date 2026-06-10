package com.xclone.post.model.enums;

import com.xclone.notification.model.enums.NotificationType;

public enum PostType {
  REPOST,
  QUOTE,
  REPLY,
  POST;

  public NotificationType toNotificationType() {
    return switch (this) {
      case REPLY -> NotificationType.REPLY;
      case REPOST -> NotificationType.REPOST;
      case QUOTE -> NotificationType.QUOTE;
      case POST -> throw new IllegalStateException("POST has no corresponding notification type");
    };
  }
}
