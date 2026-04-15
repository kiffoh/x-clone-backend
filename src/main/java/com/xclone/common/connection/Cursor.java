package com.xclone.common.connection;

import com.xclone.exception.custom.InvalidCursorException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

/**
 * Represents a pagination cursor encoding a creation timestamp and entity id.
 *
 * @param createdAt timestamp that the entity was created
 * @param id unique identifier of the entity
 */
public record Cursor(Instant createdAt, UUID id) {
  /**
   * Encodes this cursor as a base64 string in the format {@code createdAt_id}.
   *
   * @return a base64-encoded string representation of this cursor
   */
  public String encode() {
    return Base64.getEncoder().encodeToString((createdAt + "_" + id).getBytes());
  }

  /**
   * Decodes a base64 cursor string into a {@code Cursor} instance.
   *
   * @param encodedCursor base64-encoded cursor string produced by {@link Cursor#encode()}
   * @return the decoded {@code Cursor} instance
   */
  public static Cursor toCursor(String encodedCursor) {
    try {
      Base64.Decoder decoder = Base64.getDecoder();
      String[] parts = new String(decoder.decode(encodedCursor), StandardCharsets.UTF_8).split("_");
      if (parts.length != 2) {
        throw new InvalidCursorException("Malformed cursor");
      }
      Instant createdAt = Instant.parse(parts[0]);
      UUID id = UUID.fromString(parts[1]);
      return new Cursor(createdAt, id);
    } catch (IllegalArgumentException ex) {
      throw new InvalidCursorException("Malformed cursor: " + ex.getMessage());
    }
  }
}
