package com.xclone.follow.controller;

import com.xclone.exception.GraphQlErrorMapper;
import com.xclone.follow.model.entity.Follow;
import com.xclone.follow.service.FollowService;
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

  public FollowController(FollowService followService) {
    this.followService = followService;
  }

  /**
   * Follows a user account on behalf of the authenticated user.
   *
   * <p>Business exceptions are mapped with {@link GraphQlErrorMapper} in the style of
   * "errors-as-data".
   *
   * @param userDetails authenticated user; populated as part of the security chain with {@link
   *     JwtAuthenticationFilter}
   * @param userIdToFollow id of the account to be followed
   * @return the updated profile of the followed user
   */
  @MutationMapping
  public UserResponse followUser(
      @AuthenticationPrincipal CustomUserDetails userDetails, @Argument String userIdToFollow) {
    String followerId = userDetails.getUsername();
    try {
      UserProfile updatedUser =
          followService.followUser(UUID.fromString(followerId), UUID.fromString(userIdToFollow));
      return new UserResponse("201", true, updatedUser, null);
    } catch (UsernameNotFoundException ex) {
      return new UserResponse("404", false, null, GraphQlErrorMapper.fromUsernameNotFound(ex));
    }
  }

  /**
   * Unfollows a user account on behalf of the authenticated user.
   *
   * <p>Business exceptions are mapped with {@link GraphQlErrorMapper} in the style of
   * "errors-as-data".
   *
   * @param userDetails authenticated user; populated as part of the security chain with {@link
   *     JwtAuthenticationFilter}
   * @param userIdToUnfollow id of the account to be unfollowed
   * @return the updated profile of the unfollowed user
   */
  @MutationMapping
  public UserResponse unfollowUser(
      @AuthenticationPrincipal CustomUserDetails userDetails, @Argument String userIdToUnfollow) {
    String followerId = userDetails.getUsername();
    try {
      UserProfile updatedUser =
          followService.unfollowUser(
              UUID.fromString(followerId), UUID.fromString(userIdToUnfollow));
      return new UserResponse("200", true, updatedUser, null);
    } catch (UsernameNotFoundException ex) {
      return new UserResponse("404", false, null, GraphQlErrorMapper.fromUsernameNotFound(ex));
    }
  }
}
