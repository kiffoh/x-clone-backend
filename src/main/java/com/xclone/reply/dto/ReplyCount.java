package com.xclone.reply.dto;

import java.util.UUID;

/**
 * Represents the amount of direct replies a post has.
 *
 * <p>Includes an overloaded constructor to handle JPA projections, where COUNT queries return a
 * Long value.
 *
 * @param postId unique identifier of the queried post
 * @param numberOfReplies the amount of replies
 */
public record ReplyCount(UUID postId, Integer numberOfReplies) {
  public ReplyCount(UUID postId, Long numberOfReplies) {
    this(postId, numberOfReplies.intValue());
  }
}
