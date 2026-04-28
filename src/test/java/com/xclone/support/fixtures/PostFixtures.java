package com.xclone.support.fixtures;

import com.xclone.post.model.entity.Post;
import com.xclone.user.model.entity.User;
import java.util.UUID;

public class PostFixtures {
  public static Post createPostWithContent(String messageContent, User author) {
    Post post = new Post();
    post.setMessageContent(messageContent);
    post.setAuthorId(author.getId());
    return post;
  }

  public static Post createReplyWithContent(String messageContent, User author, UUID parentId) {
    Post post = new Post();
    post.setMessageContent(messageContent);
    post.setAuthorId(author.getId());
    post.setParentId(parentId);
    return post;
  }
}
