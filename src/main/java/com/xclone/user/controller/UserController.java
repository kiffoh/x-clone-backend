package com.xclone.user.controller;

import com.xclone.exception.custom.DuplicateHandleException;
import com.xclone.exception.dto.FieldError;
import com.xclone.security.user.CustomUserDetails;
import com.xclone.user.dto.UserProfile;
import com.xclone.user.dto.connection.UserConnection;
import com.xclone.user.dto.mutation.UserResponse;
import com.xclone.user.dto.request.UpdateUserInput;
import com.xclone.user.model.entity.User;
import com.xclone.user.service.UserService;
import java.util.List;
import java.util.UUID;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;

/** GraphQL controller resolving queries for the {@link com.xclone.user.model.entity.User} model. */
@Controller
public class UserController {
  private final UserService userService;

  public UserController(UserService userService) {
    this.userService = userService;
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

  // The message and success seem counter intutive to me.
  @MutationMapping
  public UserResponse updateMyProfile(
      @AuthenticationPrincipal CustomUserDetails userDetails, @Argument UpdateUserInput input) {
    User user = userDetails.getUser();
    try {
      UserProfile updatedUser = userService.updateProfile(user, input);
      return new UserResponse("200", true, updatedUser, null);
    } catch (DuplicateHandleException ex) {
      return new UserResponse(
          "409", false, null, List.of(new FieldError("handle", ex.getMessage())));
    }
  }

  @MutationMapping
  public UserResponse deleteMyAccount(@AuthenticationPrincipal CustomUserDetails userDetails) {
    User user = userDetails.getUser();
    userService.deleteProfile(user);
    return new UserResponse("200", true, null, null);
  }
}
