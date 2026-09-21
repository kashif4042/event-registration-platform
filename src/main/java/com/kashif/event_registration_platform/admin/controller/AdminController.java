package com.kashif.event_registration_platform.admin.controller;

import com.kashif.event_registration_platform.admin.dto.StatsResponse;
import com.kashif.event_registration_platform.admin.service.AdminService;
import com.kashif.event_registration_platform.auth.entity.User;
import com.kashif.event_registration_platform.common.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {
    private final AdminService adminService;
    @GetMapping("/events/{id}/stats")
    public ResponseEntity<StatsResponse> getEventStats(@PathVariable Long id,
                                                       @AuthenticationPrincipal CustomUserDetails customUserDetails) {
        StatsResponse response = adminService.getEventStats(id, customUserDetails.getUser());
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
}
