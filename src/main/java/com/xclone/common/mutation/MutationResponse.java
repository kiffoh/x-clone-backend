package com.xclone.common.mutation;

import com.xclone.exception.dto.FieldError;
import java.util.List;

/** Base interface for responses to a graphql mutation operation. */
public interface MutationResponse {
  String code();

  Boolean success();

  List<FieldError> errors();
}
