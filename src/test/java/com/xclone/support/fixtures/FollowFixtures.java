package com.xclone.support.fixtures;

import com.xclone.follow.model.entity.Follow;
import com.xclone.user.model.entity.User;

public class FollowFixtures {
  public static Follow createFollow(User follower, User following) {
    Follow f = new Follow();
    f.setFollower(follower);
    f.setFollowing(following);
    return f;
  }
}
