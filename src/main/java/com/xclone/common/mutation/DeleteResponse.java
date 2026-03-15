package com.xclone.common.mutation;

import com.xclone.exception.dto.FieldError;
import java.util.List;

public record DeleteResponse(String code, Boolean success, List<FieldError> errors)
    implements MutationResponse {}
