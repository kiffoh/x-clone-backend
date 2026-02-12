package com.xclone.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "app.auth")
@Data
public class AuthProperties {
  /**
   * Duration for access tokens in seconds.
   * Default: 900 seconds (15 minutes)
   */
  private int accessTokenDurationSeconds = 900;

  /**
   * Duration for refresh tokens in seconds.
   * Default: 2592000 seconds (30 days)
   */
  private int refreshTokenDurationSeconds = 2592000;

  /**
   * Whether to set Secure flag on cookies (HTTPS only).
   * Should be true in production, false in local dev.
   */
  private boolean secureCookies = true;
}
