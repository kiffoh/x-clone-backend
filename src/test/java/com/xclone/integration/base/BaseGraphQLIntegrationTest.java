package com.xclone.integration.base;

import com.xclone.follow.repository.FollowRepository;
import com.xclone.like.repository.LikeRepository;
import com.xclone.mention.repository.MentionRepository;
import com.xclone.notification.repository.NotificationActorRepository;
import com.xclone.notification.repository.NotificationRepository;
import com.xclone.post.repository.PostRepository;
import com.xclone.support.helpers.AuthHelpers;
import com.xclone.user.model.entity.User;
import com.xclone.user.repository.UserRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
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
  @Autowired protected NotificationRepository notificationRepository;
  @Autowired protected NotificationActorRepository notificationActorRepository;
  @Autowired protected MentionRepository mentionRepository;
  @Autowired protected AuthHelpers authHelpers;
  @Autowired protected HttpGraphQlTester authenticatedTester;
  protected User authenticatedUser;
  protected List<UUID> postIdsToDeleteFirst;

  void wipeDBs() {
    notificationActorRepository.deleteAll();
    notificationRepository.deleteAll();
    mentionRepository.deleteAll();
    likeRepository.deleteAll();
    if (postIdsToDeleteFirst != null) {
      postRepository.deleteAllById(postIdsToDeleteFirst);
    }
    postRepository.deleteAll();
    followRepository.deleteAll();
    userRepository.deleteAll();
  }

  @BeforeEach
  void setup() {
    wipeDBs();
  }

  @AfterEach
  void cleanup() {
    wipeDBs();
  }
}
