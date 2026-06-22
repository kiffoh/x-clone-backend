package com.xclone.notification.model;

import com.xclone.notification.model.enums.NotificationType;
import java.util.List;

/** Constants representing business constraint values. */
public class NotificationConstants {
  public static final int ACTOR_PREVIEW_LIMIT = 3;
  public static final long TIME_BUCKET_SECONDS = 43200; // 12 hours
  public static final List<NotificationType> UPDATABLE_NOTIFICATION_TYPES =
      List.of(NotificationType.LIKE, NotificationType.REPOST, NotificationType.FOLLOW);
  public static final List<NotificationType> ALWAYS_AGGREGATE_NOTIFICATION_TYPES =
      List.of(NotificationType.LIKE, NotificationType.REPOST);
}
