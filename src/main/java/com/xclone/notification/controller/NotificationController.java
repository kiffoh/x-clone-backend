package com.xclone.notification.controller;

import com.xclone.exception.GraphQlErrorMapper;
import com.xclone.exception.custom.NotNotificationRecipientException;
import com.xclone.exception.custom.NotificationNotFoundException;
import com.xclone.notification.dto.ActorCount;
import com.xclone.notification.dto.NotificationProfile;
import com.xclone.notification.dto.connection.NotificationConnection;
import com.xclone.notification.dto.mutation.NotificationResponse;
import com.xclone.notification.service.NotificationService;
import com.xclone.security.jwt.JwtAuthenticationFilter;
import com.xclone.security.user.CustomUserDetails;
import com.xclone.user.dto.UserProfile;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.BatchMapping;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;

/** GraphQL controller for notification-related operations. */
@Controller
public class NotificationController {
  private final NotificationService notificationService;

  public NotificationController(NotificationService notificationService) {
    this.notificationService = notificationService;
  }

  /**
   * Fetches the actors which interacted with each notification.
   *
   * @param notifications list of notification entities
   * @return a map of each notification and all the actors which have interacted with it
   */
  @BatchMapping(typeName = "Notifications", field = "actors")
  public Map<NotificationProfile, List<UserProfile>> actors(
      List<NotificationProfile> notifications) {
    List<UUID> notificationIds = notifications.stream().map(NotificationProfile::id).toList();
    Map<UUID, List<UserProfile>> notificationIdToActors =
        notificationService.getNotificationActors(notificationIds);

    return notifications.stream()
        .collect(
            Collectors.toMap(
                Function.identity(),
                notification -> notificationIdToActors.get(notification.id())));
  }

  /**
   * Fetches the count of actors which interacted with each notification.
   *
   * @param notifications list of notification entities
   * @return a map of each notification and the amount of actors which have interacted with it
   */
  @BatchMapping(typeName = "Notifications", field = "actorCount")
  public Map<NotificationProfile, Integer> actorCount(List<NotificationProfile> notifications) {
    List<UUID> notificationIds = notifications.stream().map(NotificationProfile::id).toList();
    List<ActorCount> actorCounts = notificationService.getActorCounts(notificationIds);
    Map<UUID, Integer> notificationIdToActorCount =
        actorCounts.stream()
            .collect(Collectors.toMap(ActorCount::notificationId, ActorCount::actorCount));
    return notifications.stream()
        .collect(
            Collectors.toMap(
                Function.identity(),
                notification -> notificationIdToActorCount.getOrDefault(notification.id(), 0)));
  }

  /**
   * Resolves the graphql get notifications query.
   *
   * @param userDetails authenticated user set in the security context
   * @param first optional number of notifications; defaults to 10 in graphql schema
   * @param after optional cursor for cursor-pagination
   * @return a paginated list of notifications sorted by updated date descendingly
   */
  @QueryMapping
  public NotificationConnection getNotifications(
      @AuthenticationPrincipal CustomUserDetails userDetails,
      @Argument Integer first,
      @Argument String after) {
    return notificationService.getNotifications(userDetails.getId(), first, after);
  }

  /**
   * Triggers {@link NotificationService#readNotification(UUID, UUID)} with the authenticated user.
   *
   * <p>Business exceptions are mapped with {@link GraphQlErrorMapper} in the style of
   * "errors-as-data".
   *
   * @param userDetails authenticated user; populated as part of the security chain with {@link
   *     JwtAuthenticationFilter}
   * @param notificationId unique identifier of the notification entity
   * @return the updated notification
   */
  @MutationMapping
  public NotificationResponse readNotification(
      @AuthenticationPrincipal CustomUserDetails userDetails, @Argument UUID notificationId) {
    try {
      NotificationProfile notification =
          notificationService.readNotification(userDetails.getId(), notificationId);
      return new NotificationResponse("200", true, notification, null);
    } catch (NotNotificationRecipientException ex) {
      return new NotificationResponse(
          "403", false, null, GraphQlErrorMapper.fromNotNotificationRecipient("userId", ex));
    } catch (NotificationNotFoundException ex) {
      return new NotificationResponse(
          "404", false, null, GraphQlErrorMapper.fromNotificationNotFound(ex));
    }
  }
}
