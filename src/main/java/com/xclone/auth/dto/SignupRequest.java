package com.xclone.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SignupRequest(
    @NotBlank(message = "Handle is required")
    @Size(min = 3, max = 100)
    String handle,
    @NotBlank
    @Size(min = 8, max = 100)
//  TODO:  @Pattern(regexp = \[0-9]+[a-zA-Z]+\)
    String password,
    @Size(min = 3)
    String displayName
) {
}


