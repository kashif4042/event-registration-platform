package com.kashif.event_registration_platform.event.controller;


import com.kashif.event_registration_platform.auth.entity.User;
import com.kashif.event_registration_platform.common.security.CustomUserDetails;
import com.kashif.event_registration_platform.event.dto.EventRequest;
import com.kashif.event_registration_platform.event.dto.EventResponse;
import com.kashif.event_registration_platform.event.service.EventService;
import com.kashif.event_registration_platform.registration.dto.RegistrationRequest;
import com.kashif.event_registration_platform.registration.dto.RegistrationResponse;
import com.kashif.event_registration_platform.registration.service.RegistrationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
public class EventController {
    private final EventService eventService;
    private final RegistrationService registrationService;

    @PostMapping
    public ResponseEntity<EventResponse> createEvent
            (@Valid @RequestBody EventRequest request,
             @AuthenticationPrincipal CustomUserDetails customUserDetails) {
        User organiser = customUserDetails.getUser();
        EventResponse response = eventService.createEvent(request, organiser);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PatchMapping("/{id}/publish")
    public ResponseEntity<EventResponse> publishEvent(@PathVariable Long id,
                                                      @AuthenticationPrincipal CustomUserDetails customUserDetails) {
        EventResponse response = eventService.publishEvent(id, customUserDetails.getUser());
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @PatchMapping("/{id}/unpublish")
    public ResponseEntity<EventResponse> unpublishEvent(@PathVariable Long id,
                                                        @AuthenticationPrincipal CustomUserDetails customUserDetails) {
        EventResponse response = eventService.unpublishEvent(id, customUserDetails.getUser());
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @PatchMapping("/{id}/cancel")
    public ResponseEntity<EventResponse> cancelEvent(@PathVariable Long id,
                                                     @AuthenticationPrincipal CustomUserDetails customUserDetails) {
        EventResponse response = eventService.cancelEvent(id, customUserDetails.getUser());
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @PostMapping("/{id}/register")
    public ResponseEntity<RegistrationResponse> registerForEvent(@PathVariable Long id,
                                                                 @AuthenticationPrincipal CustomUserDetails customUserDetails,
                                                                 @Valid @RequestBody RegistrationRequest request){
        RegistrationResponse response = registrationService.registerForEvent(id,request,customUserDetails.getUser());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<EventResponse>> getPublishedEvents(){
        List<EventResponse> response = eventService.getPublishedEvents();
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
}