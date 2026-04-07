package com.xclone.follow.service;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.xclone.common.connection.Cursor;
import com.xclone.follow.model.entity.Follow;
import com.xclone.follow.repository.FollowRepository;
import com.xclone.support.fixtures.FollowFixtures;
import com.xclone.support.fixtures.UserFixtures;
import com.xclone.user.dto.connection.UserConnection;
import com.xclone.user.model.entity.User;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;

@ExtendWith(MockitoExtension.class)
public class FollowServiceTests {
  @Mock FollowRepository followRepository;

  @InjectMocks FollowService followService;

  @Nested
  class getFollowersTests {
    UUID followingId = UUID.randomUUID();
    Integer first = 10;
    Pageable pageable = Pageable.ofSize(first);
    User user1 = UserFixtures.getDefaultUserWithRandomId();
    User user2 = UserFixtures.getDefaultUserWithRandomId();
    Follow follow1 = FollowFixtures.createFollow(user1, user2);
    Slice<Follow> mockFollows = new SliceImpl<>(List.of(follow1), pageable, false);

    @Test
    void afterIsNull_UserConnection() {
      when(followRepository.findFirstPageOfFollowers(eq(followingId), any(Pageable.class)))
          .thenReturn(mockFollows);

      UserConnection result = followService.getFollowers(followingId, first, null);

      assertNotNull(result);
      verify(followRepository).findFirstPageOfFollowers(eq(followingId), any(Pageable.class));
    }

    @Test
    void afterIsNotNull_UserConnection() {
      UUID cursorId = UUID.randomUUID();
      Instant cursorCreatedAt = Instant.now();
      Cursor cursor = new Cursor(cursorCreatedAt, cursorId);
      when(followRepository.findNextPageOfFollowers(
              eq(followingId), eq(cursorId), eq(cursorCreatedAt), any(Pageable.class)))
          .thenReturn(mockFollows);

      UserConnection result = followService.getFollowers(followingId, first, cursor.encode());

      assertNotNull(result);
      verify(followRepository)
          .findNextPageOfFollowers(
              eq(followingId), eq(cursorId), eq(cursorCreatedAt), any(Pageable.class));
    }
  }
}
