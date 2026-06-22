package com.xclone.mention.service;

import com.xclone.mention.dto.PostMention;
import com.xclone.mention.repository.MentionRepository;
import com.xclone.user.dto.UserProfile;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

/** Service layer for post-mention-related operations. */
@Service
public class MentionService {
  private final MentionRepository mentionRepository;

  public MentionService(MentionRepository mentionRepository) {
    this.mentionRepository = mentionRepository;
  }

  /**
   * Fetches the post mentions for each post id.
   *
   * @param postIds unique identifier for each queried post
   * @return maps each post id to the users mentioned in that posts message content.
   */
  public Map<UUID, List<UserProfile>> getPostMentions(List<UUID> postIds) {
    List<PostMention> individualMentions = mentionRepository.findPostMentions(postIds);
    return individualMentions.stream()
        .collect(
            Collectors.groupingBy(
                PostMention::postId,
                Collectors.mapping(
                    mention -> mention.user().toUserProfile(), Collectors.toList())));
  }
}
