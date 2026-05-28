package com.xclone.notification.dto.mutation;

import com.xclone.common.mutation.MutationResponse;
import com.xclone.exception.dto.FieldError;
import com.xclone.notification.dto.NotificationProfile;
import java.util.List;

/**
 * Response DTO representing the status of a mutation attempt to a notification entity.
 *
 * @param code HTTP status code
 * @param success {@code true} if the mutation completed without errors
 * @param notification nullable updated notification
 * @param errors nullable list of errors. Populated if a request fails due to business logic
 */
public record NotificationResponse(
    String code, Boolean success, NotificationProfile notification, List<FieldError> errors)
    implements MutationResponse {}
