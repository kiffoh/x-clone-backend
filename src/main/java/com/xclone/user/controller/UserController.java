package com.xclone.user.controller;

import com.xclone.common.mutation.DeleteResponse;
import com.xclone.exception.GraphQlErrorMapper;
import com.xclone.exception.custom.DuplicateHandleException;
import com.xclone.follow.service.FollowService;
import com.xclone.security.jwt.JwtAuthenticationFilter;
import com.xclone.security.user.CustomUserDetails;
import com.xclone.user.dto.UserProfile;
import com.xclone.user.dto.connection.UserConnection;
import com.xclone.user.dto.mutation.UserResponse;
import com.xclone.user.dto.request.UpdateUserInput;
import com.xclone.user.service.UserService;
import jakarta.validation.ConstraintViolationException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.BatchMapping;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;

/** GraphQL controller resolving queries for the {@link com.xclone.user.model.entity.User} model. */
@Controller
public class UserController {
  private final UserService userService;
  private final FollowService followService;

  public UserController(UserService userService, FollowService followService) {
    this.userService = userService;
    this.followService = followService;
  }

  @QueryMapping
  public UserProfile me(@AuthenticationPrincipal CustomUserDetails userDetails) {
    return userDetails.getUser().toUserProfile();
  }

  @QueryMapping
  public UserProfile userByHandle(@Argument String handle) {
    return userService.getUserByHandle(handle);
  }

  // Is this needed as I shouldn't be calling uuid publicly?
  @QueryMapping
  public UserProfile userById(@Argument UUID id) {
    return userService.getUserById(id);
  }

  @QueryMapping
  public UserConnection searchUsers(
      @Argument String query, @Argument Integer first, @Argument String after) {
    return userService.getUsersByHandle(query);
  }

  //  @QueryMapping
  //  public UserConnection suggestedUsers(
  //      @AuthenticationPrincipal CustomUserDetails userDetails,
  //      @Argument Integer first,
  //      @Argument String after) {
  //    User authenticatedUser = userDetails.getUser();
  //    return userService.getSuggestedUsers(userDetails.getUser());
  //  }

  /**
   * Triggers the {@link UserService#updateProfile(UUID, UpdateUserInput)} with the authenticated
   * user.
   *
   * <p>Business exceptions are mapped with {@link GraphQlErrorMapper} in the style of
   * "errors-as-data".
   *
   * @param userDetails authenticated user; populated as part of the security chain with {@link
   *     JwtAuthenticationFilter}
   * @param input DTO containing user details to update
   * @return the updated user
   */
  @MutationMapping
  public UserResponse updateMyProfile(
      @AuthenticationPrincipal CustomUserDetails userDetails, @Argument UpdateUserInput input) {
    try {
      UserProfile updatedUser = userService.updateProfile(userDetails.getId(), input);
      return new UserResponse("200", true, updatedUser, null);
    } catch (DuplicateHandleException ex) {
      return new UserResponse("409", false, null, GraphQlErrorMapper.fromDuplicateHandle(ex));
    } catch (ConstraintViolationException ex) {
      return new UserResponse("400", false, null, GraphQlErrorMapper.fromConstraintViolations(ex));
    }
  }

  /**
   * Triggers the {@link UserService#deleteProfile(UUID)} with the authenticated user.
   *
   * @param userDetails authenticated user; populated as part of the security chain with {@link
   *     JwtAuthenticationFilter}
   * @return the status of the soft-delete of the user
   */
  @MutationMapping
  public DeleteResponse deleteMyAccount(@AuthenticationPrincipal CustomUserDetails userDetails) {
    userService.deleteProfile(userDetails.getId());
    return new DeleteResponse("200", true, null);
  }

  @SchemaMapping(typeName = "User", field = "followers")
  private UserConnection followers(
      UserProfile user, @Argument Integer first, @Argument String after) {
    return followService.getFollowers(user.id(), first, after);
  }

  @SchemaMapping(typeName = "User", field = "followerCount")
  private long followerCount(UserProfile user) {
    return followService.getFollowerCount(user.id());
  }

  @SchemaMapping(typeName = "User", field = "following")
  private UserConnection following(
      UserProfile user, @Argument Integer first, @Argument String after) {
    return followService.getFollowing(user.id(), first, after);
  }

  @SchemaMapping(typeName = "User", field = "followingCount")
  private long followingCount(UserProfile user) {
    return followService.getFollowingCount(user.id());
  }

  @BatchMapping(typeName = "User", field = "isFollowing")
  private Map<UserProfile, Boolean> isFollowing(List<UserProfile> users) {
    CustomUserDetails userDetails =
        (CustomUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    return followService.getIsFollowing(userDetails.getId(), users);
  }
}
