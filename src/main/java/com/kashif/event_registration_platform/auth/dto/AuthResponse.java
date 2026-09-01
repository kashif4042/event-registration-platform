package com.kashif.event_registration_platform.auth.dto;

import lombok.AllArgsConstructor;
import lombok.*;
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class AuthResponse {
    private String accessToken;
    private String refreshToken;
    private UserResponse user;
}
