package com.xclone.mention.service;

import com.xclone.mention.dto.MentionDiff;
import com.xclone.mention.dto.PostMention;
import com.xclone.mention.model.entity.Mention;
import com.xclone.mention.repository.MentionRepository;
import com.xclone.user.dto.UserProfile;
import com.xclone.user.repository.UserRepository;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Service layer for post-mention-related operations. */
@Service
public class MentionService {
  private final MentionRepository mentionRepository;
  private final UserRepository userRepository;

  public MentionService(MentionRepository mentionRepository, UserRepository userRepository) {
    this.mentionRepository = mentionRepository;
    this.userRepository = userRepository;
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

  @Transactional
  public List<Mention> createMentions(UUID postId, List<UUID> mentionedUserIds) {
    List<Mention> mentionList = new ArrayList<>();
    mentionedUserIds.forEach(
        userId -> {
          boolean activeUserExists = userRepository.existsByIdAndUserStatusActive(userId);
          if (activeUserExists) {
            Mention mention = new Mention();
            mention.setPostId(postId);
            mention.setMentionedUserId(userId);
            mentionList.add(mention);
          }
        });
    if (!mentionList.isEmpty()) {
      mentionRepository.saveAll(mentionList);
    }
    return mentionList;
  }

  @Transactional
  public MentionDiff updateMentions(UUID postId, List<UUID> updatedMentionedUserIds) {
    List<Mention> currentMentions = mentionRepository.findAllByPostId(postId);
    Set<UUID> currentMentionedUserIdsSet =
        currentMentions.stream().map(Mention::getMentionedUserId).collect(Collectors.toSet());
    Set<UUID> updatedMentionedUserIdsSet = new HashSet<>(updatedMentionedUserIds);
    MentionDiff mentionDiff =
        MentionDiff.of(updatedMentionedUserIdsSet, currentMentionedUserIdsSet);
    // no change in mentions
    if (!mentionDiff.isChanged()) {
      return mentionDiff;
    }

    // create mentions which have been added
    if (!mentionDiff.added().isEmpty()) {
      createMentions(postId, mentionDiff.added());
    }
    // delete mentions which have been removed
    if (!mentionDiff.removed().isEmpty()) {
      deleteMentions(postId, mentionDiff.removed());
    }

    return mentionDiff;
  }

  @Transactional
  public void deleteMentions(UUID postId, List<UUID> mentionedUserIds) {
    for (UUID mentionedUserId : mentionedUserIds) {
      mentionRepository.deleteByPostIdAndMentionedUserId(postId, mentionedUserId);
    }
  }
}
