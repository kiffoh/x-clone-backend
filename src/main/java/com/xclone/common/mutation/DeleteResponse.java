package com.xclone.common.mutation;

import com.xclone.exception.dto.FieldError;
import java.util.List;

/**
 * Response DTO representing the status of a soft delete of a resource.
 *
 * @param code HTTP status code
 * @param success {@code true} if the mutation completed without errors
 * @param errors nullable list of errors. Populated if a request fails due to business logic
 */
public record DeleteResponse(String code, Boolean success, List<FieldError> errors)
    implements MutationResponse {}
