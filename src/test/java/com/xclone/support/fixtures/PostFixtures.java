package com.xclone.support.fixtures;

import com.xclone.post.model.entity.Post;
import com.xclone.user.model.entity.User;
import java.time.Instant;

public class PostFixtures {
  public static Post createPostWithContent(String messageContent, User author) {
    Post post = new Post();
    post.setMessageContent(messageContent);
    post.setAuthor(author);
    post.setCreatedAt(Instant.now());
    post.setUpdatedAt(Instant.now());
    return post;
  }
}
