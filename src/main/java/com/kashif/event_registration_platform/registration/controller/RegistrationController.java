package com.kashif.event_registration_platform.registration.controller;

import com.kashif.event_registration_platform.auth.entity.User;
import com.kashif.event_registration_platform.common.security.CustomUserDetails;
import com.kashif.event_registration_platform.registration.dto.RegistrationResponse;
import com.kashif.event_registration_platform.registration.service.RegistrationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/registrations")
@RequiredArgsConstructor
public class RegistrationController {
    private final RegistrationService registrationService;
    @DeleteMapping("/{id}")
    public ResponseEntity<RegistrationResponse> cancelRegistration(@PathVariable Long id,
                                                                   @AuthenticationPrincipal CustomUserDetails customUserDetails){
        return ResponseEntity.ok(registrationService.cancelRegistration(id,customUserDetails.getUser()));
    }


}
