package com.xclone.support.helpers;

import com.xclone.like.model.entity.Like;
import com.xclone.like.repository.LikeRepository;
import com.xclone.post.model.entity.Post;
import com.xclone.support.fixtures.LikeFixtures;
import com.xclone.user.model.entity.User;
import java.util.ArrayList;
import java.util.List;

public class LikeHelpers {
  public static List<Like> seedLikes(
      List<Post> posts, List<User> users, LikeRepository likeRepository) {
    if (posts.size() != users.size()) {
      throw new IllegalArgumentException("posts and users must be the same length");
    }
    List<Like> likes = new ArrayList<>();
    for (int i = 0; i < posts.size(); i++) {
      Like like = LikeFixtures.createLike(posts.get(i), users.get(i));
      Like savedLike = likeRepository.save(like);
      likes.add(savedLike);
    }
    return likes;
  }
}
