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
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
public class EventController {
    private final EventService eventService;

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
}
