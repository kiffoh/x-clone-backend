package com.xclone.mention.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/** Entity for the post mentions table. */
@Setter
@Getter
@Entity
@Table(name = "post_mentions")
@EntityListeners(AuditingEntityListener.class)
public class Mention {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "post_id", nullable = false)
  private UUID postId;

  @Column(name = "mentioned_user_id", nullable = false)
  private UUID mentionedUserId;

  @CreatedDate
  @Column(name = "created_at", nullable = false)
  private Instant createdAt;
}
