package com.xclone.security.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
    @NotBlank(message = "Handle is required")
    @Size(min = 3, max = 100)
    String handle,
    String password
) {
}
