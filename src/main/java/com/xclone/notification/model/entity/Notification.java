package com.xclone.notification.model.entity;

import com.xclone.notification.dto.NotificationProfile;
import com.xclone.notification.model.NotificationConstraintName;
import com.xclone.notification.model.enums.NotificationType;
import com.xclone.user.model.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/** Entity for the notification table. */
@Entity
@Getter
@Setter
@Table(name = "notifications")
@EntityListeners(AuditingEntityListener.class)
public class Notification {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "recipient_user_id", nullable = false)
  private UUID recipientUserId;

  /**
   * Not used in application code — notifications are always queried by the authenticated user's ID.
   * Present so that Hibernate generates the FK constraint on {@code recipient_user_id}, consistent
   * with every other foreign key in the project.
   */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(
      name = "recipient_user_id",
      insertable = false,
      updatable = false,
      foreignKey = @ForeignKey(name = NotificationConstraintName.RECIPIENT_USER_ID_FK))
  private User recipient;

  // Nullable as NotificationType.FOLLOW is not a post
  @Column(name = "post_id")
  private UUID postId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private NotificationType type;

  @Column(nullable = false)
  private boolean read = false;

  @CreatedDate
  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @LastModifiedDate
  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  /**
   * Projects this entity to a {@link NotificationProfile} for use in GraphQL responses. Timestamps
   * are converted from {@link Instant} to {@link OffsetDateTime} at UTC.
   *
   * @return immutable public-facing projection of this notification
   */
  public NotificationProfile toNotificationProfile() {
    return new NotificationProfile(
        id,
        postId,
        type,
        read,
        createdAt.atOffset(ZoneOffset.UTC),
        updatedAt.atOffset(ZoneOffset.UTC));
  }
}
