package com.xclone.user.dto.mutation;

import com.xclone.common.mutation.MutationResponse;
import com.xclone.exception.dto.FieldError;
import com.xclone.user.dto.UserProfile;
import java.util.List;

public record UserResponse(
    String code, boolean success, String message, UserProfile user, List<FieldError> errors)
    implements MutationResponse {}
