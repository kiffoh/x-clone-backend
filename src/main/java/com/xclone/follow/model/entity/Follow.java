package com.xclone.follow.model.entity;

import com.xclone.follow.model.FollowConstraintName;
import com.xclone.user.model.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Check;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/** Entity for the Follow table. */
@Getter
@Setter
@Table(
    name = "follows",
    uniqueConstraints =
        @UniqueConstraint(
            columnNames = {"follower_id", "following_id"},
            name = FollowConstraintName.FOLLOW_EXISTS))
@Check(name = FollowConstraintName.SELF_FOLLOW, constraints = "follower_id != following_id")
@Entity
@EntityListeners(AuditingEntityListener.class)
public class Follow {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @JoinColumn(name = "follower_id", nullable = false)
  @ManyToOne(fetch = FetchType.LAZY)
  private User follower;

  @JoinColumn(name = "following_id", nullable = false)
  @ManyToOne(fetch = FetchType.LAZY)
  private User following;

  @CreatedDate
  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;
}
