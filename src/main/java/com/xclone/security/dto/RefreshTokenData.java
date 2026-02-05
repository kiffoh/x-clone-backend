package com.xclone.security.dto;

import java.util.Date;

public record RefreshTokenData(String userId, Date createdAt, Date expiresAt, String deviceInfo) {
}
