package com.kashif.event_registration_platform.event.controller;


import com.kashif.event_registration_platform.auth.entity.User;
import com.kashif.event_registration_platform.common.security.CustomUserDetails;
import com.kashif.event_registration_platform.event.dto.EventRequest;
import com.kashif.event_registration_platform.event.dto.EventResponse;
import com.kashif.event_registration_platform.event.service.EventService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
public class EventController {
    private final EventService eventService;

    @PostMapping
    public ResponseEntity<EventResponse> createEvent
            (@Valid @RequestBody EventRequest request,
             @AuthenticationPrincipal CustomUserDetails customUserDetails){
        User organiser = customUserDetails.getUser();
        EventResponse response = eventService.createEvent(request,organiser);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
