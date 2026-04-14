package com.xclone.post.dto.mutation;

import com.xclone.common.mutation.MutationResponse;
import com.xclone.exception.dto.FieldError;
import com.xclone.post.dto.PostProfile;
import java.util.List;

/**
 * Response DTO representing the status of a mutation attempt to a post entity.
 *
 * @param code HTTP status code
 * @param success {@code true} if the mutation completed without errors
 * @param post nullable post profile
 * @param errors nullable list of errors. Populated if a request fails due to business logic
 */
public record PostResponse(String code, Boolean success, PostProfile post, List<FieldError> errors)
    implements MutationResponse {}
