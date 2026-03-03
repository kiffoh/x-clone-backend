package com.xclone.auth.dto;

import com.xclone.validation.ValidHandle;
import com.xclone.validation.ValidPassword;
import jakarta.validation.constraints.Size;

public record SignupRequest(
    @ValidHandle String handle,
    @ValidPassword String password,
    @Size(max = 50) String displayName,
    @Size(max = 160) String bio,
    @Size(max = 500) String profileImage) {
}
