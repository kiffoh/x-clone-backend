package com.xclone.security;

import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Configuration for Jwt signing key. */
@Configuration
public class JwtConfig {

  @Value("${jwt.secret}")
  private String jwtSecret;

  @Bean
  public SecretKey jwtSigningKey() {
    return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
  }
}
