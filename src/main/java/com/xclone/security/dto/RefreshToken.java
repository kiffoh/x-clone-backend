package com.xclone.security.dto;

import java.util.Date;
import java.util.UUID;


public class RefreshToken {
  private final UUID id;
  private final RefreshTokenData data;

  public RefreshToken(String userId, Date createdAt, Date expiresAt, String deviceInfo) {
    this.id = UUID.randomUUID();
    this.data = new RefreshTokenData(userId, createdAt, expiresAt, deviceInfo);
  }
}
