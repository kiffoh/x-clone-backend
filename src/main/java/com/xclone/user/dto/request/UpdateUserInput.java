package com.xclone.user.dto.request;

import com.xclone.validation.ValidHandle;
import jakarta.validation.constraints.Size;

public record UpdateUserInput(
    @Size(max = 50) String displayName,
    @ValidHandle String handle,
    @Size(max = 160) String bio,
    @Size(max = 500) String profileImage) {
}
