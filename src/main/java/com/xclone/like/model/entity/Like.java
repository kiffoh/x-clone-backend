package com.xclone.like.model.entity;

import com.xclone.like.model.LikeConstraintName;
import com.xclone.post.model.entity.Post;
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
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/** Entity for the likes table. */
@Entity
@Table(
    name = "likes",
    uniqueConstraints =
        @UniqueConstraint(
            columnNames = {"user_id", "post_id"},
            name = LikeConstraintName.LIKE_EXISTS))
@Getter
@Setter
@EntityListeners(AuditingEntityListener.class)
public class Like {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "user_id", nullable = false)
  private UUID userId;

  @JoinColumn(name = "user_id", insertable = false, updatable = false)
  @ManyToOne(fetch = FetchType.LAZY)
  private User user;

  @Column(name = "post_id", nullable = false)
  private UUID postId;

  @JoinColumn(
      name = "post_id",
      insertable = false,
      updatable = false,
      foreignKey = @ForeignKey(name = LikeConstraintName.LIKE_POST_FK))
  @ManyToOne(fetch = FetchType.LAZY)
  private Post post;

  @CreatedDate
  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;
}
