package com.xclone.auth.dto;

import com.xclone.validation.ValidHandle;
import com.xclone.validation.ValidPassword;

public record LoginRequest(
    @ValidHandle
    String handle,
    @ValidPassword
    String password
) {
}
