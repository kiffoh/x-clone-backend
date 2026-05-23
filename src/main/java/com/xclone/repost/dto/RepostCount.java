package com.xclone.repost.dto;

import java.util.UUID;

/**
 * Represents the combined amount of reposts and quotes a post has.
 *
 * <p>Includes an overloaded constructor to handle JPA projections, where COUNT queries return a
 * Long value.
 *
 * @param quotedPostId unique identifier of the quoted post
 * @param numberOfReposts the combined amount of reposts and quotes
 */
public record RepostCount(UUID quotedPostId, int numberOfReposts) {

  public RepostCount(UUID quotedPostId, Long numberOfReposts) {
    this(quotedPostId, numberOfReposts.intValue());
  }
}
