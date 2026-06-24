package com.xclone.mention.dto;

import com.xclone.post.model.entity.Post;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Represents the difference between the updated and current mentioned user ids of a {@link Post}.
 *
 * @param isChanged {@code false} if the updated mentions match the current mentions
 * @param added user ids added relative to the current state
 * @param removed user ids removed relative to the current state
 */
public record MentionDiff(boolean isChanged, List<UUID> added, List<UUID> removed) {

  /**
   * Computes the diff between the updated and current mentioned user ids.
   *
   * @param updated user ids which are mentioned in the updated state of the post
   * @param current user ids which are mentioned in the current state of the post
   * @return the diff between the two sets
   */
  public static MentionDiff of(Set<UUID> updated, Set<UUID> current) {
    if (updated.equals(current)) {
      return new MentionDiff(false, List.of(), List.of());
    }
    List<UUID> added = updated.stream().filter(id -> !current.contains(id)).toList();
    List<UUID> removed = current.stream().filter(id -> !updated.contains(id)).toList();
    return new MentionDiff(true, added, removed);
  }
}
