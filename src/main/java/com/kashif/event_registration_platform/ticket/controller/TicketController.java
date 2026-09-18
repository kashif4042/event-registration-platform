package com.kashif.event_registration_platform.ticket.controller;

import com.kashif.event_registration_platform.common.security.CustomUserDetails;
import com.kashif.event_registration_platform.ticket.dto.TicketResponse;
import com.kashif.event_registration_platform.ticket.service.TicketService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/tickets")
@RequiredArgsConstructor
public class TicketController {
    private final TicketService ticketService;

    @PostMapping("/{token}/checkin")
     public ResponseEntity<TicketResponse> checkIn(@AuthenticationPrincipal CustomUserDetails customUserDetails
            , @PathVariable UUID token){
        return ResponseEntity.ok(ticketService.checkIn(token,customUserDetails.getUser()));

    }
}
