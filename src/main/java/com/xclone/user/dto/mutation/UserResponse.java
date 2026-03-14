package com.xclone.user.dto.mutation;

import com.xclone.common.mutation.MutationResponse;
import com.xclone.exception.dto.FieldError;
import com.xclone.user.dto.UserProfile;
import java.util.List;

/**
 * @param code
 * @param success
 * @param user
 * @param errors
 */
public record UserResponse(String code, Boolean success, UserProfile user, List<FieldError> errors)
    implements MutationResponse {}
