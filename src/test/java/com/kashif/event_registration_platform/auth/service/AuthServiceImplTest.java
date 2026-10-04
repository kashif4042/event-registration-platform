package com.kashif.event_registration_platform.auth.service;

import com.kashif.event_registration_platform.auth.dto.AuthResponse;
import com.kashif.event_registration_platform.auth.dto.LoginRequest;
import com.kashif.event_registration_platform.auth.dto.RegisterRequest;
import com.kashif.event_registration_platform.auth.dto.UserResponse;
import com.kashif.event_registration_platform.auth.entity.Role;
import com.kashif.event_registration_platform.auth.entity.User;
import com.kashif.event_registration_platform.auth.repository.UserRepository;
import com.kashif.event_registration_platform.common.exception.DuplicateResourceException;
import com.kashif.event_registration_platform.common.exception.ResourceNotFoundException;
import com.kashif.event_registration_platform.common.security.CustomUserDetails;
import com.kashif.event_registration_platform.common.security.JwtUtil;
import com.kashif.event_registration_platform.notification.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private NotificationService notificationService;
    @Mock
    private RedisTemplate<String, String> redisTemplate;
    @Mock
    private JwtUtil jwtUtil;
    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private AuthServiceImpl authService;

    @Test
    void register_shouldSucceed_whenEmailNotTaken() {
        RegisterRequest request = new RegisterRequest();
        request.setName("Mark Henry");
        request.setEmail("mark@example.com");
        request.setPassword("password123");

        when(userRepository.existsByEmail("mark@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashedPassword");

        User savedUser = new User();
        savedUser.setId(1L);
        savedUser.setName("Mark Henry");
        savedUser.setEmail("mark@example.com");
        savedUser.setRole(Role.ATTENDEE);
        savedUser.setIsVerified(false);

        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        UserResponse response = authService.register(request);

        assertEquals("Mark Henry", response.getName());
        assertEquals("mark@example.com", response.getEmail());
        assertEquals(Role.ATTENDEE, response.getRole());
        assertFalse(response.getIsVerified());

        verify(userRepository).save(any(User.class));
        verify(notificationService).sendEmail(eq("mark@example.com"), anyString(), anyString());
    }

    @Test
    void register_shouldThrow_whenEmailAlreadyExists() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("mark@example.com");
        request.setName("Mark Henry");
        request.setPassword("password123");

        when(userRepository.existsByEmail("mark@example.com")).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> authService.register(request));

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void login_shouldReturnTokens_whenCredentialsAreValid() {
        LoginRequest request = new LoginRequest();
        request.setEmail("mark@example.com");
        request.setPassword("password123");

        User user = new User();
        user.setId(1L);
        user.setName("Mark Henry");
        user.setEmail("mark@example.com");
        user.setRole(Role.ATTENDEE);
        user.setIsVerified(false);

        when(userRepository.findByEmail("mark@example.com")).thenReturn(Optional.of(user));
        when(jwtUtil.generateAccessToken(any(CustomUserDetails.class))).thenReturn("access-token");
        when(jwtUtil.generateRefreshToken(any(CustomUserDetails.class))).thenReturn("refresh-token");

        AuthResponse response = authService.login(request);

        assertEquals("access-token", response.getAccessToken());
        assertEquals("refresh-token", response.getRefreshToken());
        assertEquals("mark@example.com", response.getUser().getEmail());
        assertEquals("refresh-token", user.getRefreshToken());

        verify(authenticationManager).authenticate(any());
        verify(userRepository).save(user);
    }

    @Test
    void login_shouldThrowResourceNotFound_whenCredentialsAreInvalid() {

        LoginRequest request = new LoginRequest();
        request.setEmail("mark@example.com");
        request.setPassword("wrongpassword");

        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("bad credentials"));

        assertThrows(ResourceNotFoundException.class, () -> authService.login(request));

        verify(userRepository, never()).findByEmail(anyString());
        verify(userRepository, never()).save(any(User.class));
    }
}