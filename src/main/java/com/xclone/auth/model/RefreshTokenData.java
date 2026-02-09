package com.xclone.auth.model;

import java.util.Date;

public record RefreshTokenData(String userId, Date createdAt, Date expiresAt, String deviceInfo) {
}
