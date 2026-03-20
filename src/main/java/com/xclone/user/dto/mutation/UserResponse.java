package com.xclone.user.dto.mutation;

import com.xclone.common.mutation.MutationResponse;
import com.xclone.exception.dto.FieldError;
import com.xclone.user.dto.UserProfile;
import java.util.List;

/**
 * Response DTO representing the status of a mutation attempt to the User entity.
 *
 * @param code HTTP status code
 * @param success {@code true} if the mutation completed without errors
 * @param user nullable updated user
 * @param errors nullable list of errors. Populated if a request fails due to business logic
 */
public record UserResponse(String code, Boolean success, UserProfile user, List<FieldError> errors)
    implements MutationResponse {}
