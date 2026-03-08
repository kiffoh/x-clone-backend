package com.xclone.user.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record UserProfile(
    UUID id,
    String handle,
    String displayName,
    String bio,
    String profileImage,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt) {}
