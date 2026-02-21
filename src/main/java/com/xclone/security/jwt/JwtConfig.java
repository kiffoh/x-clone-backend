package com.xclone.security.jwt;

import com.xclone.config.JwtProperties;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import javax.crypto.SecretKey;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for Jwt signing key.
 */
@Configuration
public class JwtConfig {
  private final JwtProperties jwtProperties;

  JwtConfig(JwtProperties jwtProperties) {
    this.jwtProperties = jwtProperties;
  }

  @Bean
  public SecretKey jwtSigningKey() {
    return Keys.hmacShaKeyFor(
        this.jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8)
    );
  }
}
