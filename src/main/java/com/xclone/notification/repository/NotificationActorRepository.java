package com.xclone.notification.repository;

import com.xclone.notification.model.entity.NotificationActor;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** JPA repository for {@link NotificationActor} entities. */
@Repository
public interface NotificationActorRepository extends JpaRepository<NotificationActor, UUID> {
  void deleteByActorUserIdAndNotificationId(UUID authenticatedUserId, UUID id);

  long countByNotificationId(UUID notificationId);

  void deleteAllByNotificationIdIn(List<UUID> postNotificationIds);
}
