package com.xclone.support.fixtures;

import com.xclone.like.model.entity.Like;
import com.xclone.post.model.entity.Post;
import com.xclone.user.model.entity.User;

public class LikeFixtures {
  /**
   * Creates a like entity with queried post and user.
   *
   * @param post post entity to add a like to
   * @param user user entity which issued the like
   * @return a like entity with the postId and userId set
   */
  public static Like createLike(Post post, User user) {
    Like like = new Like();
    like.setPostId(post.getId());
    like.setUserId(user.getId());
    return like;
  }
}
