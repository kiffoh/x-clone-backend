package com.xclone.support.fixtures;

import com.xclone.post.model.entity.Post;
import com.xclone.user.model.entity.User;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class PostFixtures {
  public static Post createPostWithContent(String messageContent, User author) {
    Post post = new Post();
    post.setMessageContent(messageContent);
    post.setAuthorId(author.getId());
    return post;
  }

  public static List<String> magpieRhyme =
      Arrays.asList(
          "One for sorrow",
          "Two for joy",
          "Three for a girl",
          "Four for a boy",
          "Five for silver",
          "Six for gold",
          "Seven for a secret never to be told",
          "Eight for a wish",
          "Nine for a kiss",
          "Ten for a bird you must not miss");

  public static Post createReplyWithContent(String messageContent, User author, UUID parentId) {
    Post post = new Post();
    post.setMessageContent(messageContent);
    post.setAuthorId(author.getId());
    post.setParentId(parentId);
    return post;
  }

  public static Post createQuote(UUID quotedPostId, User author, String messageContent) {
    Post post = new Post();
    post.setMessageContent(messageContent);
    post.setAuthorId(author.getId());
    post.setQuotedPostId(quotedPostId);
    return post;
  }

  public static Post createRepost(UUID quotedPostId, User author) {
    Post post = new Post();
    post.setAuthorId(author.getId());
    post.setQuotedPostId(quotedPostId);
    return post;
  }
}
