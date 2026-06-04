package com.xclone.notification.dto;

import java.util.UUID;

/**
 * Represents the amount of actors a notification has.
 *
 * <p>Includes an overloaded constructor to handle JPA projections, where COUNT queries return a
 * Long value.
 *
 * @param notificationId unique identifier of the notification
 * @param actorCount the amount of actors
 */
public record ActorCount(UUID notificationId, int actorCount) {
  public ActorCount(UUID notificationId, Long actorCount) {
    this(notificationId, actorCount.intValue());
  }
}
