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
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AuthServiceImplTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private NotificationService notificationService;
    @Mock private RedisTemplate<String, String> redisTemplate;
    @Mock private JwtUtil jwtUtil;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private ValueOperations<String, String> valueOperations;

    @InjectMocks private AuthServiceImpl authService;

    private User buildUser() {
        User user = new User();
        user.setId(1L);
        user.setName("Mark Henry");
        user.setEmail("mark@example.com");
        user.setPasswordHash("oldHash");
        user.setRole(Role.ATTENDEE);
        user.setIsVerified(false);
        return user;
    }

    // ---------- register ----------

    @Test
    void register_shouldSucceed_whenEmailNotTaken() {
        RegisterRequest request = new RegisterRequest();
        request.setName("Mark Henry");
        request.setEmail("mark@example.com");
        request.setPassword("password123");

        when(userRepository.existsByEmail("mark@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashedPassword");
        when(userRepository.save(any(User.class))).thenReturn(buildUser());
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
        request.setName("Mark Henry");
        request.setEmail("mark@example.com");
        request.setPassword("password123");

        when(userRepository.existsByEmail("mark@example.com")).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> authService.register(request));
        verify(userRepository, never()).save(any(User.class));
    }

    // ---------- login ----------

    @Test
    void login_shouldReturnTokens_whenCredentialsAreValid() {
        LoginRequest request = new LoginRequest();
        request.setEmail("mark@example.com");
        request.setPassword("password123");
        User user = buildUser();

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

    // ---------- refreshToken ----------

    @Test
    void refreshToken_shouldIssueNewTokens_whenTokenIsValid() {
        RefreshRequest request = new RefreshRequest();
        request.setRefreshToken("old-refresh-token");
        User user = buildUser();
        user.setRefreshToken("old-refresh-token");

        when(jwtUtil.extractUsername("old-refresh-token")).thenReturn("mark@example.com");
        when(userRepository.findByEmail("mark@example.com")).thenReturn(Optional.of(user));
        when(jwtUtil.isTokenValid(eq("old-refresh-token"), any(CustomUserDetails.class))).thenReturn(true);
        when(jwtUtil.generateAccessToken(any(CustomUserDetails.class))).thenReturn("new-access-token");
        when(jwtUtil.generateRefreshToken(any(CustomUserDetails.class))).thenReturn("new-refresh-token");

        AuthResponse response = authService.refreshToken(request);

        assertEquals("new-access-token", response.getAccessToken());
        assertEquals("new-refresh-token", response.getRefreshToken());
        assertEquals("new-refresh-token", user.getRefreshToken());
        verify(userRepository).save(user);
    }

    @Test
    void refreshToken_shouldThrow_whenTokenDoesNotMatchStoredOne() {
        RefreshRequest request = new RefreshRequest();
        request.setRefreshToken("stale-token");
        User user = buildUser();
        user.setRefreshToken("current-token");

        when(jwtUtil.extractUsername("stale-token")).thenReturn("mark@example.com");
        when(userRepository.findByEmail("mark@example.com")).thenReturn(Optional.of(user));

        assertThrows(ResourceNotFoundException.class, () -> authService.refreshToken(request));
        verify(jwtUtil, never()).isTokenValid(any(), any());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void refreshToken_shouldThrow_whenTokenIsNotValid() {
        RefreshRequest request = new RefreshRequest();
        request.setRefreshToken("expired-token");
        User user = buildUser();
        user.setRefreshToken("expired-token");

        when(jwtUtil.extractUsername("expired-token")).thenReturn("mark@example.com");
        when(userRepository.findByEmail("mark@example.com")).thenReturn(Optional.of(user));
        when(jwtUtil.isTokenValid(eq("expired-token"), any(CustomUserDetails.class))).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> authService.refreshToken(request));
        verify(jwtUtil, never()).generateAccessToken(any());
    }

    // ---------- verifyEmail ----------

    @Test
    void verifyEmail_shouldMarkUserVerified_andDeleteToken() {
        User user = buildUser();
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("token123")).thenReturn("mark@example.com");
        when(userRepository.findByEmail("mark@example.com")).thenReturn(Optional.of(user));

        authService.verifyEmail("token123");

        assertTrue(user.getIsVerified());
        verify(userRepository).save(user);
        verify(redisTemplate).delete("token123");
    }

    @Test
    void verifyEmail_shouldThrow_whenTokenExpiredOrUnknown() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("expired")).thenReturn(null);

        assertThrows(ResourceNotFoundException.class, () -> authService.verifyEmail("expired"));
        verify(userRepository, never()).findByEmail(anyString());
        verify(userRepository, never()).save(any(User.class));
    }

    // ---------- forgotPassword ----------

    @Test
    void forgotPassword_shouldStoreResetToken_andEmailUser_whenUserExists() {
        ForgotPasswordRequest request = new ForgotPasswordRequest();
        request.setEmail("mark@example.com");

        when(userRepository.findByEmail("mark@example.com")).thenReturn(Optional.of(buildUser()));
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        authService.forgotPassword(request);

        verify(valueOperations).set(startsWith("reset:"), eq("mark@example.com"), eq(15L), eq(TimeUnit.MINUTES));
        verify(notificationService).sendEmail(eq("mark@example.com"), anyString(), anyString());
    }

    @Test
    void forgotPassword_shouldDoNothingAndNotThrow_whenUserDoesNotExist() {
        ForgotPasswordRequest request = new ForgotPasswordRequest();
        request.setEmail("ghost@example.com");

        when(userRepository.findByEmail("ghost@example.com")).thenReturn(Optional.empty());

        // enumeration safety: same outward behaviour whether or not the email exists
        assertDoesNotThrow(() -> authService.forgotPassword(request));
        verify(redisTemplate, never()).opsForValue();
        verify(notificationService, never()).sendEmail(anyString(), anyString(), anyString());
    }

    // ---------- resetPassword ----------

    @Test
    void resetPassword_shouldUpdatePassword_andDeleteToken() {
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setToken("abc");
        request.setNewPassword("newPassword123");
        User user = buildUser();

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("reset:abc")).thenReturn("mark@example.com");
        when(userRepository.findByEmail("mark@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("newPassword123")).thenReturn("newHash");

        authService.resetPassword(request);

        assertEquals("newHash", user.getPasswordHash());
        verify(userRepository).save(user);
        verify(redisTemplate).delete("reset:abc");
    }

    @Test
    void resetPassword_shouldThrow_whenTokenInvalidOrExpired() {
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setToken("bad");
        request.setNewPassword("newPassword123");

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("reset:bad")).thenReturn(null);

        assertThrows(ResourceNotFoundException.class, () -> authService.resetPassword(request));
        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).save(any(User.class));
    }
}