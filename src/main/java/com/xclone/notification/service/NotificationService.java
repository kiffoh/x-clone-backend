package com.xclone.notification.service;

import com.xclone.common.connection.Cursor;
import com.xclone.common.connection.PageInfo;
import com.xclone.exception.custom.NotNotificationRecipientException;
import com.xclone.exception.custom.NotificationNotFoundException;
import com.xclone.notification.dto.ActorCount;
import com.xclone.notification.dto.NotificationProfile;
import com.xclone.notification.dto.connection.NotificationConnection;
import com.xclone.notification.dto.connection.NotificationEdge;
import com.xclone.notification.model.NotificationConstants;
import com.xclone.notification.model.NotificationConstraintName;
import com.xclone.notification.model.entity.Notification;
import com.xclone.notification.model.entity.NotificationActor;
import com.xclone.notification.model.enums.NotificationType;
import com.xclone.notification.repository.NotificationActorRepository;
import com.xclone.notification.repository.NotificationRepository;
import com.xclone.user.dto.UserProfile;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.postgresql.util.PSQLException;
import org.postgresql.util.ServerErrorMessage;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Service layer responsible for notification-related operations. */
@Slf4j
@Service
public class NotificationService {
  private final NotificationRepository notificationRepository;
  private final NotificationActorRepository notificationActorRepository;
  private final Clock clock;

  public NotificationService(
      NotificationRepository notificationRepository,
      NotificationActorRepository notificationActorRepository,
      Clock clock) {
    this.notificationRepository = notificationRepository;
    this.notificationActorRepository = notificationActorRepository;
    this.clock = clock;
  }

  /**
   * Fetches the {@link NotificationConnection} for the queried notification id.
   *
   * @param userId unique identifier of the recipient (authenticated user)
   * @param first optional number of notifications; defaults to 10 in graphql schema
   * @param after optional cursor for cursor-pagination
   * @return a paginated list of notifications sorted by updated date descendingly
   */
  public NotificationConnection getNotifications(UUID userId, Integer first, String after) {
    Pageable pageable = Pageable.ofSize(first);
    Slice<Notification> notifications;
    if (after == null) {
      notifications = notificationRepository.findFirstPageOfNotifications(userId, pageable);
    } else {
      Cursor cursor = Cursor.toCursor(after);
      notifications =
          notificationRepository.findNextPageOfNotifications(
              userId, cursor.timestamp(), cursor.id(), pageable);
    }
    return toNotificationConnection(notifications);
  }

  /**
   * Fetches the actors which interacted with each notification.
   *
   * <p>Limits the actors fetched to the most recent 3.
   *
   * @param notificationIds list of unique identifiers of notification entities
   * @return a map of each notification id and the most recent 3 actors which have interacted with
   *     it
   */
  public Map<UUID, List<UserProfile>> getMostRecentNotificationActors(List<UUID> notificationIds) {
    List<NotificationActor> notificationActors =
        notificationRepository.findNotificationActors(notificationIds);
    Map<UUID, List<UserProfile>> notificationIdToActors = new HashMap<>();
    notificationActors.forEach(
        notificationActor -> {
          List<UserProfile> users =
              notificationIdToActors.computeIfAbsent(
                  notificationActor.getNotificationId(), k -> new ArrayList<>());

          if (users.size() < NotificationConstants.ACTOR_PREVIEW_LIMIT) {
            users.add(notificationActor.getActor().toUserProfile());
          }
        });

    return notificationIdToActors;
  }

  public List<ActorCount> getActorCounts(List<UUID> notificationIds) {
    return notificationRepository.findActorCounts(notificationIds);
  }

  /**
   * Updates the read status of the notification entity to {@code read=true} using a {@link
   * Transactional} view, ensuring for atomicity and that dirty checking applies.
   *
   * <p>Only the recipient of a notification can update the read status
   *
   * @param userId unique identifier of the authenticated user
   * @param notificationId unique identifier of the notification
   * @return notification with the read status as true
   * @throws NotNotificationRecipientException when the recipient of the notification does not match
   *     the userId parameter
   * @throws NotificationNotFoundException when notification cannot be found in the database
   */
  @Transactional
  public NotificationProfile readNotification(UUID userId, UUID notificationId) {
    Notification notification =
        notificationRepository
            .findById(notificationId)
            .orElseThrow(() -> new NotificationNotFoundException("Notification does not exist"));

    if (!userId.equals(notification.getRecipientUserId())) {
      throw new NotNotificationRecipientException("Only the recipient can read the notification");
    }

    notification.setRead(true);

    return notification.toNotificationProfile();
  }

  /**
   * Creates or updates a notification depending on the {@link NotificationType}.
   *
   * <ul>
   *   <li>A {@link NotificationType#QUOTE}, {@link NotificationType#MENTION}, {@link
   *       NotificationType#REPLY} will always create a new notification.
   *   <li>A {@link NotificationType#LIKE} or {@link NotificationType#REPOST} will always update the
   *       existing notification.
   *   <li>A {@link NotificationType#FOLLOW} will update the existing notification if within the
   *       time-window defined in {@link NotificationConstants#TIME_BUCKET_SECONDS}; else, it will
   *       create a new notification.
   * </ul>
   *
   * <p>Note: Self-notification guard is present, and early returns if met.
   *
   * <p>{@link Transactional} view is used for updating an existing notification. A notification is
   * updated when a new {@link NotificationActor} is created which references the {@code
   * notification.id}. A notification update involves:
   *
   * <ul>
   *   <li>{@code notification.updatedAt} set to the current timestamp.
   *   <li>{@code notification.read} set to {@code false} to reset the notification read status .
   * </ul>
   */
  @Transactional
  public void upsertNotification(
      UUID recipientId, UUID authenticatedUserId, UUID postId, NotificationType type) {

    if (recipientId.equals(authenticatedUserId)) {
      // Don't trigger notification in the case of a self action i.e. liked own post
      return;
    }
    if (NotificationConstants.UPDATABLE_NOTIFICATION_TYPES.contains(type)) {
      Optional<Notification> existingNotification;
      if (type == NotificationType.FOLLOW) {
        existingNotification = notificationRepository.findLastUpdatedFollow(recipientId);
      } else if (NotificationConstants.ALWAYS_AGGREGATE_NOTIFICATION_TYPES.contains(type)) {
        existingNotification =
            notificationRepository.findAggregateNotification(recipientId, postId, type);
      } else {
        existingNotification =
            notificationRepository.findDiscreteNotification(
                recipientId, authenticatedUserId, postId, type);
      }

      if (existingNotification.isPresent()) {
        Instant now = Instant.now(clock);
        long lastUpdatedSince =
            now.getEpochSecond() - existingNotification.get().getUpdatedAt().getEpochSecond();
        // flags
        boolean likeOrRepost =
            NotificationConstants.ALWAYS_AGGREGATE_NOTIFICATION_TYPES.contains(type);
        boolean followInsideTimeBucket =
            (type == NotificationType.FOLLOW)
                && (lastUpdatedSince < NotificationConstants.TIME_BUCKET_SECONDS);
        if (likeOrRepost || followInsideTimeBucket) {
          createActorAndUpdateNotification(authenticatedUserId, existingNotification.get(), now);
          return;
        }
      }
    }

    try {
      Notification notification = createNotification(recipientId, type, postId);
      createNotificationActor(notification.getId(), authenticatedUserId);
    } catch (DataIntegrityViolationException ex) {
      if (ex.contains(PSQLException.class)) {
        PSQLException psql = (PSQLException) ex.getRootCause();
        if (psql != null) {
          ServerErrorMessage serverError = psql.getServerErrorMessage();
          if (serverError != null) {
            String constraintName = serverError.getConstraint();
            if (NotificationConstraintName.ONE_LIKE_NOTIFICATION.equals(constraintName)
                || NotificationConstraintName.ONE_REPOST_NOTIFICATION.equals(constraintName)) {
              log.debug(
                  "Duplicate aggregate notification swallowed (concurrent race): {}",
                  constraintName);
              return;
            }
          }
        }
      }
      throw ex;
    }
  }

  private Notification createNotification(UUID recipientId, NotificationType type, UUID postId) {
    Notification notification = new Notification();
    notification.setRecipientUserId(recipientId);
    notification.setType(type);
    if (postId != null) {
      notification.setPostId(postId);
    }
    return notificationRepository.saveAndFlush(notification);
  }

  private void createNotificationActor(UUID notificationId, UUID authenticatedUserId) {
    NotificationActor notificationActor = new NotificationActor();
    notificationActor.setNotificationId(notificationId);
    notificationActor.setActorUserId(authenticatedUserId);
    notificationActorRepository.save(notificationActor);
  }

  private Notification createActorAndUpdateNotification(
      UUID authenticatedUserId, Notification existingNotification, Instant now) {
    createNotificationActor(existingNotification.getId(), authenticatedUserId);
    existingNotification.setUpdatedAt(now);
    existingNotification.setRead(false);
    return notificationRepository.save(existingNotification);
  }

  /**
   * Deletes the actor corresponding to the input parameters and consequently deletes the
   * corresponding notification if there are no associated actors.
   *
   * @param recipientId unique identifier of the recipient of the notification
   * @param authenticatedUserId unique identifier of the actor of the notification
   * @param postId optional unique identifier of the post the notification is associated with
   * @param type type of notification (e.g. which domain it was triggered from)
   */
  @Transactional
  public void deleteNotificationActorAndCleanupNotification(
      UUID recipientId, UUID authenticatedUserId, UUID postId, NotificationType type) {
    Optional<Notification> notification;
    if (type == NotificationType.FOLLOW) {
      // postId is only null for a follow
      notification =
          notificationRepository.findSpecificFollowNotification(recipientId, authenticatedUserId);
    } else {
      notification =
          notificationRepository.findDiscreteNotification(
              recipientId, authenticatedUserId, postId, type);
    }
    if (notification.isEmpty()) {
      // No notification to clean up so fails silently
      return;
    }
    UUID notificationId = notification.get().getId();
    notificationActorRepository.deleteByActorUserIdAndNotificationId(
        authenticatedUserId, notificationId);
    // Clean up notification if there are no actors remaining
    long remaining = notificationActorRepository.countByNotificationId(notificationId);
    if (remaining == 0) {
      notificationRepository.deleteById(notificationId);
    }
  }

  private NotificationConnection toNotificationConnection(Slice<Notification> notifications) {
    List<NotificationEdge> edges =
        notifications.stream()
            .map(
                notification -> {
                  Cursor cursor = new Cursor(notification.getUpdatedAt(), notification.getId());
                  return new NotificationEdge(
                      notification.toNotificationProfile(), cursor.encode());
                })
            .toList();
    PageInfo pageInfo =
        new PageInfo(
            notifications.hasNext(),
            notifications.hasPrevious(),
            edges.isEmpty() ? null : edges.getFirst().cursor(),
            edges.isEmpty() ? null : edges.getLast().cursor());
    return new NotificationConnection(edges, pageInfo);
  }

  /**
   * Deletes all notification data and the corresponding notification actor data with a post id that
   * matches the queried id.
   *
   * <p>Uses a {@link Transactional} view for an atomic delete.
   *
   * @param postId unique identifier of the post
   */
  @Transactional
  public void deletePostNotifications(UUID postId) {
    List<Notification> notifications = notificationRepository.findAllByPostId(postId);
    List<UUID> notificationIds = notifications.stream().map(Notification::getId).toList();
    notificationActorRepository.deleteAllByNotificationIdIn(notificationIds);
    notificationRepository.deleteAll(notifications);
  }
}
