package com.kashif.event_registration_platform.registration.dto;


import com.kashif.event_registration_platform.registration.entity.RegistrationStatus;

import jakarta.annotation.Nullable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RegistrationResponse {
    private Long id;
    private Long eventId;
    private RegistrationStatus status;
    private LocalDateTime registeredAt;
    private Long teamRegistrationId;

}
