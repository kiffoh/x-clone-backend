package com.xclone.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Generates and validates JWT tokens.
 */
@Service
public class JwtTokenProvider {
  private final SecretKey jwtSigningKey;

  @Value("${jwt.expiration}")
  private final long jwtExpiration;

  public JwtTokenProvider(SecretKey jwtSigningKey, @Value("${jwt.expiration}") long jwtExpiration) {
    this.jwtSigningKey = jwtSigningKey;
    this.jwtExpiration = jwtExpiration;
  }

  /**
   * Generates a JWT Token.
   */
  public String createToken(String userId, String role) {
    Date now = new Date();
    Date expiration = new Date(now.getTime() + this.jwtExpiration);
    return Jwts.builder().subject(userId).issuedAt(now).expiration(expiration).claim("role", role)
        .signWith(this.jwtSigningKey).compact();
  }

  public Claims parseToken(String jws) {
    return Jwts.parser().verifyWith(this.jwtSigningKey).build().parseSignedClaims(jws).getPayload();
  }

  public String getUserIdFromToken(String jws) {
    return Jwts.parser().verifyWith(this.jwtSigningKey).build().parseSignedClaims(jws).getPayload()
        .getSubject();
  }

  /**
   * Validates a JWT Token.
   */
  public boolean validToken(String jws) {
    try {
      Claims token = parseToken(jws);
      return token.getExpiration().getTime() > new Date().getTime();
    } catch (Exception e) {
      return false;
    }
  }
}
