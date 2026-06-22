package com.xclone.follow.controller;

import com.xclone.exception.GraphQlErrorMapper;
import com.xclone.exception.custom.AccountNotActiveException;
import com.xclone.exception.custom.DuplicateFollowException;
import com.xclone.exception.custom.SelfFollowException;
import com.xclone.follow.model.entity.Follow;
import com.xclone.follow.service.FollowService;
import com.xclone.notification.model.enums.NotificationType;
import com.xclone.notification.service.NotificationService;
import com.xclone.security.jwt.JwtAuthenticationFilter;
import com.xclone.security.user.CustomUserDetails;
import com.xclone.user.dto.UserProfile;
import com.xclone.user.dto.mutation.UserResponse;
import java.util.UUID;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Controller;

/** GraphQL controller resolving queries for the {@link Follow} model. */
@Controller
public class FollowController {
  private final FollowService followService;
  private final NotificationService notificationService;

  public FollowController(FollowService followService, NotificationService notificationService) {
    this.followService = followService;
    this.notificationService = notificationService;
  }

  /**
   * Follows a user account on behalf of the authenticated user.
   *
   * <p>Business exceptions are mapped with {@link GraphQlErrorMapper} as per the ethos of
   * "errors-as-data".
   *
   * @param userDetails authenticated user; populated as part of the security chain with {@link
   *     JwtAuthenticationFilter}
   * @param userIdToFollow id of the account to be followed
   * @return the updated profile of the followed user
   */
  @MutationMapping
  public UserResponse followUser(
      @AuthenticationPrincipal CustomUserDetails userDetails, @Argument UUID userIdToFollow) {
    UUID followerId = userDetails.getId();
    try {
      UserProfile updatedUser = followService.followUser(followerId, userIdToFollow);
      notificationService.upsertNotification(
          userIdToFollow, followerId, null, NotificationType.FOLLOW);
      return new UserResponse("201", true, updatedUser, null);
    } catch (UsernameNotFoundException ex) {
      return new UserResponse(
          "404", false, null, GraphQlErrorMapper.fromUsernameNotFound("userIdToFollow", ex));
    } catch (DuplicateFollowException ex) {
      return new UserResponse("409", false, null, GraphQlErrorMapper.fromDuplicateFollow(ex));
    } catch (SelfFollowException ex) {
      return new UserResponse("400", false, null, GraphQlErrorMapper.fromSelfFollow(ex));
    } catch (AccountNotActiveException ex) {
      return new UserResponse(
          "409", false, null, GraphQlErrorMapper.fromAccountNotActive("userIdToFollow", ex));
    }
  }

  /**
   * Unfollows a user account on behalf of the authenticated user.
   *
   * <p>Business exceptions are mapped with {@link GraphQlErrorMapper} as per the ethos of
   * "errors-as-data".
   *
   * @param userDetails authenticated user; populated as part of the security chain with {@link
   *     JwtAuthenticationFilter}
   * @param userIdToUnfollow id of the account to be unfollowed
   * @return the updated profile of the unfollowed user
   */
  @MutationMapping
  public UserResponse unfollowUser(
      @AuthenticationPrincipal CustomUserDetails userDetails, @Argument UUID userIdToUnfollow) {
    UUID followerId = userDetails.getId();
    try {
      UserProfile updatedUser = followService.unfollowUser(followerId, userIdToUnfollow);
      notificationService.deleteNotificationActorAndCleanupNotification(
          userDetails.getId(), userIdToUnfollow, NotificationType.FOLLOW, null);

      return new UserResponse("200", true, updatedUser, null);
    } catch (UsernameNotFoundException ex) {
      return new UserResponse(
          "404", false, null, GraphQlErrorMapper.fromUsernameNotFound("userIdToUnfollow", ex));
    }
  }
}
