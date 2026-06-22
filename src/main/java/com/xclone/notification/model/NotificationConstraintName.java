package com.xclone.notification.model;

/** Constants representing business constraint names for notifications and notification actors. */
public class NotificationConstraintName {
  public static final String ACTOR_USER_ID_FK = "notification_constraint_actor_user_id_fk";
  public static final String NOTIFICATION_ID_FK = "notification_constraint_notification_id_fk";
  public static final String RECIPIENT_USER_ID_FK = "notification_constraint_recipient_user_id_fk";
  public static final String ONE_LIKE_NOTIFICATION = "one_like_notification_per_recipient";
  public static final String ONE_REPOST_NOTIFICATION = "one_repost_notification_per_recipient";
}
