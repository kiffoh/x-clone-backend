package com.xclone.like.dto;

import java.util.UUID;

/**
 * Represents the amount of likes a post has.
 *
 * <p>Includes an overloaded constructor to handle JPA projections, where COUNT queries return a
 * Long value.
 *
 * @param postId unique identifier of the post
 * @param numberOfLikes the amount of likes
 */
public record LikeCount(UUID postId, int numberOfLikes) {

  public LikeCount(UUID postId, Long numberOfLikes) {
    this(postId, numberOfLikes.intValue());
  }
}
