package com.xclone.integration.base;

import com.xclone.follow.repository.FollowRepository;
import com.xclone.like.repository.LikeRepository;
import com.xclone.post.repository.PostRepository;
import com.xclone.support.helpers.AuthHelpers;
import com.xclone.user.model.entity.User;
import com.xclone.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.graphql.tester.AutoConfigureHttpGraphQlTester;
import org.springframework.context.annotation.Import;
import org.springframework.graphql.test.tester.HttpGraphQlTester;

@AutoConfigureHttpGraphQlTester
@Import(AuthHelpers.class)
public class BaseGraphQLIntegrationTest extends BaseIntegrationTest {
  @Autowired protected UserRepository userRepository;
  @Autowired protected FollowRepository followRepository;
  @Autowired protected LikeRepository likeRepository;
  @Autowired protected PostRepository postRepository;
  @Autowired protected AuthHelpers authHelpers;
  @Autowired protected HttpGraphQlTester authenticatedTester;
  protected User authenticatedUser;

  @BeforeEach
  void wipeDBs() {
    likeRepository.deleteAll();
    postRepository.deleteAll();
    followRepository.deleteAll();
    userRepository.deleteAll();
  }
}
