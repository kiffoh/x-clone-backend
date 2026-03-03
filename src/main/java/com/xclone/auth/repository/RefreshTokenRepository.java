package com.xclone.auth.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xclone.auth.model.RefreshTokenData;
import com.xclone.config.AuthProperties;
import com.xclone.exception.custom.InvalidRefreshTokenException;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

/**
 * Repository responsible for persisting and retrieving refresh token metadata in Redis.
 *
 * <p>Each token is stored under a key of the form: {@code refresh_token:{uuid}}
 *
 * <p>Entries are stored with a TTL equal to the configured refresh token lifetime.
 */
@Repository
public class RefreshTokenRepository {
  private final StringRedisTemplate redisTemplate;
  private final AuthProperties authProperties;
  private final ObjectMapper objectMapper;

  public RefreshTokenRepository(
      StringRedisTemplate redisTemplate, AuthProperties authProperties, ObjectMapper objectMapper) {
    this.redisTemplate = redisTemplate;
    this.authProperties = authProperties;
    this.objectMapper = objectMapper;
  }

  /**
   * Writes the new refresh token to the redis database.
   *
   * @param tokenId opaque refresh token id
   * @param metadata refresh token metadata
   */
  public void save(String tokenId, RefreshTokenData metadata) {
    try {
      String serializedData = objectMapper.writeValueAsString(metadata);
      this.redisTemplate
          .opsForValue()
          .set(
              key(tokenId),
              serializedData,
              authProperties.getRefreshTokenDurationSeconds(),
              TimeUnit.SECONDS);
    } catch (JsonProcessingException e) {
      throw new RuntimeException("Failed to serialize token data", e);
    }
  }

  /**
   * Returns the refresh token metadata based on the token id.
   *
   * @param tokenId opaque refresh token id
   * @return refresh token metadata
   */
  public RefreshTokenData find(String tokenId) {
    String tokenData =
        Optional.ofNullable(this.redisTemplate.opsForValue().get(key(tokenId)))
            .orElseThrow(
                () ->
                    new InvalidRefreshTokenException(
                        String.format("Token with an ID of %s does not exist", tokenId)));
    try {
      return objectMapper.readValue(tokenData, RefreshTokenData.class);
    } catch (JsonProcessingException e) {
      throw new RuntimeException("Corrupted token data in Redis for key: " + key(tokenId), e);
    }
  }

  public void delete(String tokenId) {
    this.redisTemplate.delete(key(tokenId));
  }

  private String key(String tokenId) {
    return "refresh_token:" + tokenId;
  }
}
