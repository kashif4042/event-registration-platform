package com.kashif.event_registration_platform.auth.service;

import com.kashif.event_registration_platform.auth.dto.AuthResponse;
import com.kashif.event_registration_platform.auth.dto.LoginRequest;
import com.kashif.event_registration_platform.auth.dto.RegisterRequest;
import com.kashif.event_registration_platform.auth.dto.UserResponse;

public interface AuthService {
    UserResponse register(RegisterRequest request);
    AuthResponse login(LoginRequest request);
}
