package com.xclone.support.fixtures;

import com.xclone.post.model.entity.Post;
import com.xclone.user.model.entity.User;

public class PostFixtures {
  public static Post createPostWithContent(String messageContent, User author) {
    Post post = new Post();
    post.setMessageContent(messageContent);
    post.setAuthorId(author.getId());
    return post;
  }
}
