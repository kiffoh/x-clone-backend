package com.xclone.share.dto;

import java.util.UUID;

/**
 * Represents the combined amount of reposts and quotes a post has.
 *
 * <p>Includes an overloaded constructor to handle JPA projections, where COUNT queries return a
 * Long value.
 *
 * @param sharedPostId unique identifier of the shared post
 * @param numberOfShares the combined amount of reposts and quotes
 */
public record ShareCount(UUID sharedPostId, int numberOfShares) {

  public ShareCount(UUID sharedPostId, Long numberOfShares) {
    this(sharedPostId, numberOfShares.intValue());
  }
}
