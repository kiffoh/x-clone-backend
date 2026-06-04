package com.xclone.notification.repository;

import com.xclone.notification.dto.ActorCount;
import com.xclone.notification.model.entity.Notification;
import com.xclone.notification.model.entity.NotificationActor;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/** JPA repository for {@link Notification} entities. */
@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {

  @Query(
      "select n from Notification n where n.recipientUserId = :userId"
          + " order by n.updatedAt desc, n.id asc")
  Slice<Notification> findFirstPageOfNotifications(@Param("userId") UUID userId, Pageable pageable);

  @Query(
      "select n from Notification n where n.recipientUserId = :userId"
          + " and ((n.updatedAt < :cursorTimestamp) or "
          + "(n.updatedAt = :cursorTimestamp and n.id > :cursorId))"
          + " order by n.updatedAt desc, n.id asc")
  Slice<Notification> findNextPageOfNotifications(
      @Param("userId") UUID userId,
      @Param("cursorTimestamp") Instant cursorTimestamp,
      @Param("cursorId") UUID cursorId,
      Pageable pageable);

  @Query(
      "select na from NotificationActor na join fetch na.actor"
          + " where na.notificationId in :notificationIds order by na.createdAt desc")
  List<NotificationActor> findNotificationActors(
      @Param("notificationIds") List<UUID> notificationIds);

  @Query(
      "select new com.xclone.notification.dto.ActorCount(na.notificationId, count(na))"
          + " from NotificationActor na where na.notificationId in :notificationIds"
          + " group by na.notificationId")
  List<ActorCount> findActorCounts(@Param("notificationIds") List<UUID> notificationIds);
}
