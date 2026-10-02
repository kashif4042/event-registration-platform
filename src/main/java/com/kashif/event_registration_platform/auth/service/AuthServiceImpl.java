package com.kashif.event_registration_platform.auth.service;

import com.kashif.event_registration_platform.auth.dto.*;
import com.kashif.event_registration_platform.auth.entity.Role;
import com.kashif.event_registration_platform.auth.entity.User;
import com.kashif.event_registration_platform.auth.repository.UserRepository;
import com.kashif.event_registration_platform.common.exception.DuplicateResourceException;
import com.kashif.event_registration_platform.common.exception.ResourceNotFoundException;
import com.kashif.event_registration_platform.common.security.CustomUserDetails;
import com.kashif.event_registration_platform.common.security.JwtUtil;
import com.kashif.event_registration_platform.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;
    private final RedisTemplate<String, String> redisTemplate;
    private final NotificationService notificationService;


    @Override
    public UserResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Email already exists");
        }
        User user = new User();
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setRole(Role.ATTENDEE);
        user.setIsVerified(false);
        user.setCreatedAt(LocalDateTime.now());

        User savedUser = userRepository.save(user);
        String token = UUID.randomUUID().toString();
        redisTemplate.opsForValue().set(
                token,
                savedUser.getEmail(),
                15,
                TimeUnit.MINUTES
        );
        notificationService.sendEmail(
                savedUser.getEmail(),
                "Verify your email",
                "Click this link to verify: http://localhost:8080/api/auth/verify?token=" + token
        );
        return new UserResponse(savedUser.getId(), savedUser.getName(), savedUser.getEmail(), savedUser.getRole(), savedUser.getIsVerified());
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );
        } catch (BadCredentialsException e) {
            throw new ResourceNotFoundException("Invalid email or password");
        }

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("Invalid Email or Password"));

        CustomUserDetails customUserDetails = new CustomUserDetails(user);

        String accessToken = jwtUtil.generateAccessToken(customUserDetails);
        String refreshToken = jwtUtil.generateRefreshToken(customUserDetails);

        user.setRefreshToken(refreshToken);
        userRepository.save(user);
        UserResponse userResponse = new UserResponse(user.getId(), user.getName(), user.getEmail(), user.getRole(), user.getIsVerified());
        return new AuthResponse(accessToken, refreshToken, userResponse);
    }

    @Override
    public AuthResponse refreshToken(RefreshRequest request) {
        String email = jwtUtil.extractUsername(request.getRefreshToken());

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Invalid refresh token"));
        if (!request.getRefreshToken().equals(user.getRefreshToken())) {
            throw new ResourceNotFoundException("Invalid refresh token");
        }
        CustomUserDetails customUserDetails = new CustomUserDetails(user);
        if (!jwtUtil.isTokenValid(request.getRefreshToken(), customUserDetails)) {
            throw new ResourceNotFoundException("Invalid refresh token");
        }

        String newAccessToken = jwtUtil.generateAccessToken(customUserDetails);
        String newRefreshToken = jwtUtil.generateRefreshToken(customUserDetails);

        user.setRefreshToken(newRefreshToken);
        userRepository.save(user);

        UserResponse userResponse = new UserResponse(user.getId(), user.getName(), user.getEmail(), user.getRole(), user.getIsVerified());
        return new AuthResponse(newAccessToken, newRefreshToken, userResponse);

    }

    @Override
    public void verifyEmail(String token) {
        String email = redisTemplate.opsForValue().get(token);

        if (email == null) {
            throw new ResourceNotFoundException("Invalid or expired verification token");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        user.setIsVerified(true);
        userRepository.save(user);

        redisTemplate.delete(token);
    }

    @Override
    public void forgotPassword(ForgotPasswordRequest request) {
        Optional<User> userOptional = userRepository.findByEmail(request.getEmail());
        if (userOptional.isPresent()) {
            User user = userOptional.get();
            String token = UUID.randomUUID().toString();
            redisTemplate.opsForValue().set("reset:" + token, user.getEmail(), 15, TimeUnit.MINUTES);
            notificationService.sendEmail(
                    user.getEmail(),
                    "Reset your password",
                    "Use this token to reset your password: " + token
            );
        }
    }

    @Override
    public void resetPassword(ResetPasswordRequest request){
        String key = "reset:" + request.getToken();
        String email = redisTemplate.opsForValue().get(key);
        if(email == null){
            throw new ResourceNotFoundException("Invalid or expired reset token");
        }
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        redisTemplate.delete(key);
    }
}
