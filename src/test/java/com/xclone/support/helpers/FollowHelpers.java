package com.xclone.support.helpers;

import static com.xclone.support.fixtures.FollowFixtures.createFollow;

import com.xclone.follow.model.entity.Follow;
import com.xclone.follow.repository.FollowRepository;
import com.xclone.user.model.entity.User;

public class FollowHelpers {
  public static void seedFollow(FollowRepository followRepository, User follower, User following) {
    Follow f = createFollow(follower, following);
    followRepository.save(f);
  }
}
