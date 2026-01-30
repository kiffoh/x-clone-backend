package com.xclone.model.entity;

import com.xclone.model.enums.UserRole;
import com.xclone.model.enums.UserStatus;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/** Entity for users table */
@Getter
@Setter
@Entity
@Table(name = "users")
@EntityListeners(AuditingEntityListener.class)
public class User {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "display_name", nullable = false, length = 100)
  private String displayName;

  @Column(nullable = false, length = 100, unique = true)
  private String handle;

  @Column(name = "password_hash", nullable = false, length = 255)
  private String passwordHash;

  @Column(columnDefinition = "TEXT")
  private String bio;

  @Column(name = "profile_image", length = 500)
  private String profileImage;

  // Where do the enums live? In this class?
  // Should the enum values be protected so I can access in other classes e.g. CustomUserDetails

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private UserStatus status = UserStatus.ACTIVE;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private UserRole role = UserRole.USER;

  @CreatedDate
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt; // Is it correct to use java.sql here?

  @LastModifiedDate
  @Column(name = "updated_at")
  private LocalDateTime updatedAt;

  public User() {
    this.id = UUID.randomUUID();
  }
}
