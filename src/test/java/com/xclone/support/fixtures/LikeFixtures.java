package com.xclone.support.fixtures;

import com.xclone.like.model.entity.Like;
import com.xclone.post.model.entity.Post;
import com.xclone.user.model.entity.User;

public class LikeFixtures {
  public static Like createLike(Post post, User user) {
    Like like = new Like();
    like.setPostId(post.getId());
    like.setUserId(user.getId());
    return like;
  }
}
