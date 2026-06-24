package com.xclone.support.fixtures;

import com.xclone.mention.model.entity.Mention;
import com.xclone.post.model.entity.Post;
import com.xclone.user.model.entity.User;

public class MentionFixtures {
  public static Mention createMention(Post post, User user) {
    Mention mention = new Mention();
    mention.setPostId(post.getId());
    mention.setMentionedUserId(user.getId());
    return mention;
  }
}
