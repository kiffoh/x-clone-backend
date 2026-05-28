package com.xclone.notification.dto;

import com.xclone.notification.model.entity.Notification;
import com.xclone.notification.model.enums.NotificationType;
import com.xclone.post.model.entity.Post;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Immutable public-facing projection of a {@link Notification} entity, mapping to the {@code
 * Notification} type in the GraphQL schema.
 *
 * @param id unique identifier of the notification
 * @param post optional post entity which specific notification types act upon
 * @param type enum corresponding to the notification type
 * @param read {@code true} if the recipient user has read the notification
 * @param createdAt datetime of the creation of the notification
 * @param updatedAt datetime of last update of the notification
 */
public record NotificationProfile(
    UUID id,
    Post post,
    NotificationType type,
    boolean read,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt) {}
