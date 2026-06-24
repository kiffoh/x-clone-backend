package com.xclone.mention.dto;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public record MentionDiff(boolean isChanged, List<UUID> added, List<UUID> removed) {
  public static MentionDiff of(Set<UUID> updated, Set<UUID> current) {
    if (updated.equals(current)) {
      return new MentionDiff(false, null, null);
    }
    List<UUID> added = updated.stream().filter(id -> !current.contains(id)).toList();
    List<UUID> removed = current.stream().filter(id -> !updated.contains(id)).toList();
    return new MentionDiff(true, added, removed);
  }
}
