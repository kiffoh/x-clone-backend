package com.xclone.notification.service;

import com.xclone.common.connection.Cursor;
import com.xclone.common.connection.PageInfo;
import com.xclone.exception.custom.NotNotificationRecipientException;
import com.xclone.exception.custom.NotificationNotFoundException;
import com.xclone.notification.dto.ActorCount;
import com.xclone.notification.dto.NotificationProfile;
import com.xclone.notification.dto.connection.NotificationConnection;
import com.xclone.notification.dto.connection.NotificationEdge;
import com.xclone.notification.model.entity.Notification;
import com.xclone.notification.model.entity.NotificationActor;
import com.xclone.notification.repository.NotificationRepository;
import com.xclone.user.dto.UserProfile;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Service layer responsible for notification-related operations. */
@Service
public class NotificationService {
  private final NotificationRepository notificationRepository;

  public NotificationService(NotificationRepository notificationRepository) {
    this.notificationRepository = notificationRepository;
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
            edges.getFirst().cursor(),
            edges.getLast().cursor());
    return new NotificationConnection(edges, pageInfo);
  }

  /**
   * Fetches the actors which interacted with each notification.
   *
   * @param notificationIds list of unique identifiers of notification entities
   * @return a map of each notification id and all the actors which have interacted with it
   */
  public Map<UUID, List<UserProfile>> getNotificationActors(List<UUID> notificationIds) {
    List<NotificationActor> notificationActors =
        notificationRepository.findNotificationActors(notificationIds);
    Map<UUID, List<UserProfile>> notificationIdToActors = new HashMap<>();
    notificationActors.forEach(
        notificationActor ->
            notificationIdToActors
                .computeIfAbsent(notificationActor.getNotificationId(), k -> new ArrayList<>())
                .add(notificationActor.getActor().toUserProfile()));
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
      throw new NotNotificationRecipientException("Only the recipient can read the post");
    }

    notification.setRead(true);

    return notification.toNotificationProfile();
  }
}
