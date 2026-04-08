package com.xclone.support.helpers;

import com.xclone.post.model.entity.Post;
import com.xclone.post.repository.PostRepository;
import com.xclone.support.fixtures.PostFixtures;
import com.xclone.user.model.entity.User;
import java.util.ArrayList;
import java.util.List;

public class PostHelpers {
  public static List<Post> seedPosts(
      List<String> messageContents, List<User> authors, PostRepository postRepository) {
    List<Post> posts = new ArrayList<>();
    for (int i = 0; i < messageContents.size(); i++) {
      Post post = PostFixtures.createPostWithContent(messageContents.get(i), authors.get(i));
      Post savedPost = postRepository.save(post);
      posts.add(savedPost);
    }
    return posts;
  }
}
