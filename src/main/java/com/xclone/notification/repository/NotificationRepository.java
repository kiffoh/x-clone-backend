package com.xclone.notification.repository;

import com.xclone.notification.dto.ActorCount;
import com.xclone.notification.model.NotificationConstants;
import com.xclone.notification.model.entity.Notification;
import com.xclone.notification.model.entity.NotificationActor;
import com.xclone.notification.model.enums.NotificationType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
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

  /**
   * Finds a notification for a discrete type ({@link NotificationType#QUOTE}, {@link
   * NotificationType#REPLY}, {@link NotificationType#MENTION}), where each actor creates a separate
   * notification. Joins on {@link NotificationActor} for additional specificity as multiple
   * notifications can share the same {@code (recipient, post, type)}.
   *
   * @param recipientId unique identifier of the user who receives the notification
   * @param actorId unique identifier of the user who generated the notification
   * @param postId unique identifier of the post
   * @param type notification type
   * @return optional notification if found
   */
  @Query(
      "select n from Notification n join NotificationActor na on n.id = na.notificationId"
          + " where n.recipientUserId = :recipientId and na.actorUserId = :actorId"
          + " and n.postId = :postId and n.type = :type order by n.updatedAt desc limit 1")
  Optional<Notification> findDiscreteNotification(
      @Param("recipientId") UUID recipientId,
      @Param("actorId") UUID actorId,
      @Param("postId") UUID postId,
      @Param("type") NotificationType type);

  /**
   * Finds the shared notification for an aggregate type ({@link NotificationType#LIKE}, {@link
   * NotificationType#REPOST}), where all actors share a single notification per post. No actor join
   * is needed because {@code (recipient, post, type)} uniquely identifies the notification.
   *
   * @param recipientId unique identifier of the user who receives the notification
   * @param postId unique identifier of the post
   * @param type notification type
   * @return optional notification if found
   */
  @Query(
      "select n from Notification n where n.recipientUserId = :recipientId"
          + " and n.postId = :postId and n.type = :type")
  Optional<Notification> findAggregateNotification(
      @Param("recipientId") UUID recipientId,
      @Param("postId") UUID postId,
      @Param("type") NotificationType type);

  /**
   * Finds the most recently updated follow notification for a recipient. Used to check whether the
   * notification's {@code updatedAt} falls within the aggregation window defined by {@link
   * NotificationConstants#TIME_BUCKET_SECONDS}.
   *
   * @param recipientId unique identifier of the user who receives the notification
   * @return optional notification if found
   */
  @Query(
      "select n from Notification n where n.recipientUserId = :recipientId"
          + " and n.postId is null"
          + " and n.type = com.xclone.notification.model.enums.NotificationType.FOLLOW"
          + " order by n.updatedAt desc limit 1")
  Optional<Notification> findLastUpdatedFollow(@Param("recipientId") UUID recipientId);

  /**
   * Finds the follow notification containing a given actor. Joins on {@link NotificationActor}
   * because the target notification may not be the most recent — {@link #findLastUpdatedFollow}
   * would miss it if newer follow notifications exist.
   *
   * @param recipientId unique identifier of the user who receives the notification
   * @param actorId unique identifier of the user who generated the notification
   * @return optional notification if found
   */
  @Query(
      "select n from Notification n join NotificationActor na on n.id = na.notificationId"
          + " where n.recipientUserId = :recipientId and n.postId is null"
          + " and n.type = com.xclone.notification.model.enums.NotificationType.FOLLOW"
          + " and na.actorUserId = :actorId")
  Optional<Notification> findSpecificFollowNotification(
      @Param("recipientId") UUID recipientId, @Param("actorId") UUID actorId);

  List<Notification> findAllByPostId(UUID postId);
}
