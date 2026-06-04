package com.xclone.support.fixtures;

import com.xclone.notification.model.entity.Notification;
import com.xclone.notification.model.entity.NotificationActor;
import com.xclone.notification.model.enums.NotificationType;
import java.time.Instant;
import java.util.UUID;

public class NotificationFixtures {
  public static Notification createNotification(
      UUID recipientUserId, UUID postId, NotificationType type, Instant createdAt) {
    Notification notification = new Notification();
    if (postId != null) {
      notification.setPostId(postId);
    }
    notification.setType(type);
    notification.setRecipientUserId(recipientUserId);
    notification.setCreatedAt(createdAt);
    return notification;
  }

  public static NotificationActor createNotificationActor(
      UUID actorId, UUID notificationId, Instant createdAt) {
    NotificationActor notificationActor = new NotificationActor();
    notificationActor.setActorUserId(actorId);
    notificationActor.setNotificationId(notificationId);
    notificationActor.setCreatedAt(createdAt);
    return notificationActor;
  }
}
