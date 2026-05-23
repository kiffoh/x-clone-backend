package com.xclone.share.dto;

import java.util.UUID;

/**
 * Represents the combined amount of reposts and quotes a post has.
 *
 * <p>Includes an overloaded constructor to handle JPA projections, where COUNT queries return a
 * Long value.
 *
 * @param quotedPostId unique identifier of the quoted post
 * @param numberOfShares the combined amount of reposts and quotes
 */
public record ShareCount(UUID quotedPostId, int numberOfShares) {

  public ShareCount(UUID quotedPostId, Long numberOfShares) {
    this(quotedPostId, numberOfShares.intValue());
  }
}
