package com.xclone.support.fixtures;

import com.xclone.like.model.entity.Like;
import com.xclone.post.model.entity.Post;
import com.xclone.user.model.entity.User;
import java.time.Instant;

public class LikeFixtures {
  /**
   * Creates a like entity with queried post and user and a deterministic created at.
   *
   * @param post post entity to add a like to
   * @param user user entity which issued the like
   * @param createdAt datetime when the like was created
   * @return a like entity with the postId and userId set
   */
  public static Like createLike(Post post, User user, Instant createdAt) {
    Like like = new Like();
    like.setPostId(post.getId());
    like.setUserId(user.getId());
    like.setCreatedAt(createdAt);
    return like;
  }
}
