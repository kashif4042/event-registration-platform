package com.kashif.event_registration_platform.auth.service;

import com.kashif.event_registration_platform.auth.dto.*;

public interface AuthService {
    UserResponse register(RegisterRequest request);
    AuthResponse login(LoginRequest request);
    AuthResponse refreshToken(RefreshRequest request);
}
