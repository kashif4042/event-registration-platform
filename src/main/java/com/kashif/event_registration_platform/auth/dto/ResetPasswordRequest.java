package com.kashif.event_registration_platform.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;



@Getter
@Setter
public class ResetPasswordRequest {
    private String token;
    @Size(min = 7 , message = "Password must be at least 7 characters")
    @NotBlank(message = "Password is required")
    private String newPassword;
}
