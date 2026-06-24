package com.xclone.mention.dto;

import com.xclone.post.dto.PostProfile;
import com.xclone.user.model.entity.User;
import java.util.UUID;

/**
 * Represents a single relationship between a post and a user which is mentioned in the {@link
 * PostProfile#messageContent()}.
 *
 * @param postId unique identifier of the post
 * @param user user who is mentioned in the message content of the post
 */
public record PostMention(UUID postId, User user) {}
