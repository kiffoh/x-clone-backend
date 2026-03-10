package com.xclone.common.mutation;

import com.xclone.exception.dto.FieldError;
import java.util.List;

public interface MutationResponse {
  String code();

  boolean success();

  String message();

  List<FieldError> errors();
}
