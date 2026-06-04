package com.xclone.support.helpers;

import static com.xclone.support.fixtures.NotificationFixtures.createNotification;
import static com.xclone.support.fixtures.NotificationFixtures.createNotificationActor;

import com.xclone.notification.model.entity.Notification;
import com.xclone.notification.model.entity.NotificationActor;
import com.xclone.notification.model.enums.NotificationType;
import com.xclone.notification.repository.NotificationActorRepository;
import com.xclone.notification.repository.NotificationRepository;
import com.xclone.post.model.entity.Post;
import com.xclone.user.model.entity.User;
import java.time.Instant;
import java.util.List;

public class NotificationHelpers {

  public static Notification seedNotifications(
      User recipient,
      Post post,
      NotificationType type,
      List<User> actors,
      NotificationRepository notificationRepository,
      NotificationActorRepository notificationActorRepository) {
    Instant now = Instant.now();
    Notification notification;
    if (post != null) {
      notification = createNotification(recipient.getId(), post.getId(), type, now);
    } else {
      notification = createNotification(recipient.getId(), null, type, now);
    }
    notification = notificationRepository.save(notification);
    for (int i = 0; i < actors.size(); i++) {
      NotificationActor notificationActor =
          createNotificationActor(actors.get(i).getId(), notification.getId(), now.plusSeconds(i));
      notificationActorRepository.save(notificationActor);
    }
    return notification;
  }
}
