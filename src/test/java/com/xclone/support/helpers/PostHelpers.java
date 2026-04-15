package com.xclone.support.helpers;

import com.xclone.post.model.entity.Post;
import com.xclone.post.repository.PostRepository;
import com.xclone.support.fixtures.PostFixtures;
import com.xclone.user.model.entity.User;
import java.util.ArrayList;
import java.util.List;

public class PostHelpers {
  /**
   * Seeds one post per index position across messageContents and authors.
   *
   * @param messageContents list of strings containing each posts text content
   * @param authors list of users which each post belongs to
   * @param postRepository interface for connecting Post entities to the database
   * @return list of posts from database
   * @throws IllegalArgumentException if messageContents and authors are different lengths
   */
  public static List<Post> seedPosts(
      List<String> messageContents, List<User> authors, PostRepository postRepository) {
    if (messageContents.size() != authors.size()) {
      throw new IllegalArgumentException("messageContents and authors must be the same length");
    }
    List<Post> posts = new ArrayList<>();
    for (int i = 0; i < messageContents.size(); i++) {
      Post post = PostFixtures.createPostWithContent(messageContents.get(i), authors.get(i));
      Post savedPost = postRepository.save(post);
      posts.add(savedPost);
    }
    return posts;
  }
}
