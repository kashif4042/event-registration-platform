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
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;

    @Override
    public UserResponse register(RegisterRequest request){
        if(userRepository.existsByEmail(request.getEmail())){
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
        return new UserResponse(savedUser.getId(), savedUser.getName(), savedUser.getEmail(), savedUser.getRole(), savedUser.getIsVerified());
    }

    @Override
    public AuthResponse login(LoginRequest request){
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );
        } catch (BadCredentialsException e) {
            throw new ResourceNotFoundException("Invalid email or password");
        }

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(()-> new ResourceNotFoundException("Invalid Email or Password"));

        CustomUserDetails customUserDetails = new CustomUserDetails(user);

        String accessToken = jwtUtil.generateAccessToken(customUserDetails);
        String refreshToken = jwtUtil.generateRefreshToken(customUserDetails);

        user.setRefreshToken(refreshToken);
        userRepository.save(user);
        UserResponse userResponse = new UserResponse(user.getId(), user.getName(), user.getEmail(), user.getRole(), user.getIsVerified());
        return new AuthResponse(accessToken, refreshToken, userResponse);
        }
    }

