package com.xclone.security.jwt;

import com.xclone.config.AuthProperties;
import com.xclone.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import java.util.Date;
import javax.crypto.SecretKey;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Generates and validates JWT tokens.
 */
@Slf4j
@Component
public class JwtTokenProvider {
  private final SecretKey jwtSigningKey;
  private final AuthProperties authProperties;
  private final JwtProperties jwtProperties;

  public JwtTokenProvider(SecretKey jwtSigningKey,
                          AuthProperties authProperties,
                          JwtProperties jwtProperties) {
    this.jwtSigningKey = jwtSigningKey;
    this.authProperties = authProperties;
    this.jwtProperties = jwtProperties;
  }

  /**
   * Creates a JWT access token.
   * Algorithm is automatically selected based on key length.
   *
   * @param userId User's unique identifier
   * @param role   User's role (USER, ADMIN)
   * @return Signed JWT string
   */
  public String createToken(String userId, String role) {
    Date now = new Date();
    Date expiration = new Date(
        now.getTime() + this.authProperties.getAccessTokenDurationSeconds() * 1000L
    );
    return Jwts.builder()
        .subject(userId)
        .issuer(jwtProperties.getIssuer())
        .issuedAt(now)
        .expiration(expiration)
        .claim("role", role)
        .signWith(this.jwtSigningKey)
        .compact();
  }

  /**
   * Parses and validates a JWT token.
   */
  public Claims parseToken(String jws) {
    return Jwts.parser()
        .verifyWith(this.jwtSigningKey)
        .build()
        .parseSignedClaims(jws)
        .getPayload();
  }

  /**
   * Extracts user ID from token.
   */
  public String getUserIdFromToken(String jws) {
    return Jwts.parser()
        .verifyWith(this.jwtSigningKey)
        .build()
        .parseSignedClaims(jws)
        .getPayload()
        .getSubject();
  }

  /**
   * Validates a JWT Token.
   */
  public boolean validToken(String jws) {
    try {
      Claims claims = parseToken(jws);

      // Validate issuer
      if (!jwtProperties.getIssuer().equals(claims.getIssuer())) {
        log.warn("Invalid issuer: expected={}, actual={}",
            jwtProperties.getIssuer(), claims.getIssuer());
        return false;
      }

      // Validate expiration
      return claims.getExpiration().after(new Date());

    } catch (Exception e) {
      log.debug("Token validation failed: {}", e.getMessage());
      return false;
    }
  }

}
