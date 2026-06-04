package com.xclone.notification.controller;

import com.xclone.exception.GraphQlErrorMapper;
import com.xclone.exception.custom.NotNotificationRecipientException;
import com.xclone.exception.custom.NotificationNotFoundException;
import com.xclone.notification.dto.ActorCount;
import com.xclone.notification.dto.NotificationProfile;
import com.xclone.notification.dto.connection.NotificationConnection;
import com.xclone.notification.dto.mutation.NotificationResponse;
import com.xclone.notification.model.enums.NotificationType;
import com.xclone.notification.service.NotificationService;
import com.xclone.post.dto.PostProfile;
import com.xclone.post.service.PostService;
import com.xclone.security.jwt.JwtAuthenticationFilter;
import com.xclone.security.user.CustomUserDetails;
import com.xclone.user.dto.UserProfile;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
  private final PostService postService;

  public NotificationController(NotificationService notificationService, PostService postService) {
    this.notificationService = notificationService;
    this.postService = postService;
  }

  /**
   * Fetches the post relevant to each notification.
   *
   * <p>For a notification with {@link NotificationType#FOLLOW}, the post will be null.
   *
   * @param notifications list of notification entities
   * @return a map of each notification and the post which the notification acted upon
   */
  @BatchMapping(typeName = "Notification", field = "post")
  public Map<NotificationProfile, PostProfile> post(List<NotificationProfile> notifications) {
    List<UUID> postIds =
        notifications.stream()
            .map(NotificationProfile::postId)
            // If NotificationType == FOLLOW then postId (and consequently post) is null
            .filter(Objects::nonNull)
            .toList();
    List<PostProfile> posts = postService.getActivePostsFromIds(postIds);
    Map<UUID, PostProfile> postIdToPostMap =
        posts.stream().collect(Collectors.toMap(PostProfile::id, Function.identity()));

    Map<NotificationProfile, PostProfile> notificationPosts = new HashMap<>();
    notifications.forEach(
        notificationProfile -> {
          notificationPosts.put(
              notificationProfile, postIdToPostMap.get(notificationProfile.postId()));
        });
    return notificationPosts;
  }

  /**
   * Fetches the 3 most recent actors which interacted with each notification.
   *
   * @param notifications list of notification entities
   * @return a map of each notification and the 3 latest actors which have interacted with it
   */
  @BatchMapping(typeName = "Notification", field = "actors")
  public Map<NotificationProfile, List<UserProfile>> actors(
      List<NotificationProfile> notifications) {
    List<UUID> notificationIds = notifications.stream().map(NotificationProfile::id).toList();
    Map<UUID, List<UserProfile>> notificationIdToActors =
        notificationService.getMostRecent3NotificationActors(notificationIds);

    return notifications.stream()
        .collect(
            Collectors.toMap(
                Function.identity(),
                notification -> notificationIdToActors.getOrDefault(notification.id(), List.of())));
  }

  /**
   * Fetches the count of actors which interacted with each notification.
   *
   * @param notifications list of notification entities
   * @return a map of each notification and the amount of actors which have interacted with it
   */
  @BatchMapping(typeName = "Notification", field = "actorCount")
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
