package com.xclone.post.model.entity;

import com.xclone.common.enums.Status;
import com.xclone.post.dto.PostProfile;
import com.xclone.post.model.PostConstraintName;
import com.xclone.user.model.entity.User;
import com.xclone.validation.ValidationConstants;
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

/** Entity for the posts table. */
@Getter
@Setter
@Entity
@Table(name = "posts")
@EntityListeners(AuditingEntityListener.class)
public class Post {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "author_id", nullable = false)
  private UUID authorId;

  @JoinColumn(name = "author_id", insertable = false, updatable = false)
  @ManyToOne(fetch = FetchType.LAZY)
  private User author;

  // TODO add nullable=true when reposts are added
  @Column(name = "message_content", length = ValidationConstants.MAX_MESSAGE_CONTENT_SIZE)
  private String messageContent;

  @CreatedDate
  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @LastModifiedDate
  @Column(name = "updated_at")
  private Instant updatedAt;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private Status status = Status.ACTIVE;

  @Column(name = "parent_id")
  private UUID parentId;

  @JoinColumn(
      name = "parent_id",
      insertable = false,
      updatable = false,
      foreignKey = @ForeignKey(name = PostConstraintName.POST_PARENT_FK))
  @ManyToOne(fetch = FetchType.LAZY)
  private Post parent;

  @Column(name = "quoted_post_id")
  private UUID quotedPostId;

  @JoinColumn(
      name = "quoted_post_id",
      insertable = false,
      updatable = false,
      foreignKey = @ForeignKey(name = PostConstraintName.QUOTED_POST_FK))
  @ManyToOne(fetch = FetchType.LAZY)
  private Post quotedPost;

  /**
   * Projects this entity to a {@link PostProfile} for use in GraphQL responses. Timestamps are
   * converted from {@link Instant} to {@link OffsetDateTime} at UTC.
   *
   * @return immutable public-facing projection of this post
   */
  public PostProfile toPostProfile() {
    return new PostProfile(
        id,
        authorId,
        messageContent,
        parentId,
        createdAt.atOffset(ZoneOffset.UTC),
        updatedAt.atOffset(ZoneOffset.UTC));
  }
}
