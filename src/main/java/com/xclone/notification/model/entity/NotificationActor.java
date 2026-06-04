package com.xclone.notification.model.entity;

import com.xclone.notification.model.NotificationConstraintName;
import com.xclone.user.model.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/** Entity for the notification actors table. */
@Entity
@Getter
@Setter
@Table(name = "notification_actors")
@EntityListeners(AuditingEntityListener.class)
public class NotificationActor {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "actor_user_id", nullable = false)
  private UUID actorUserId;

  @JoinColumn(
      name = "actor_user_id",
      insertable = false,
      updatable = false,
      foreignKey = @ForeignKey(name = NotificationConstraintName.ACTOR_USER_ID_FK))
  @ManyToOne(fetch = FetchType.LAZY)
  private User actor;

  @Column(name = "notification_id", nullable = false)
  private UUID notificationId;

  @JoinColumn(
      name = "notification_id",
      insertable = false,
      updatable = false,
      foreignKey = @ForeignKey(name = NotificationConstraintName.NOTIFICATION_ID_FK))
  @ManyToOne(fetch = FetchType.LAZY)
  private Notification notification;

  @CreatedDate
  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;
}
