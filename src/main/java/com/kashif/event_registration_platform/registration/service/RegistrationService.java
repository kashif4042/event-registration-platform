package com.kashif.event_registration_platform.registration.service;

import com.kashif.event_registration_platform.auth.entity.User;
import com.kashif.event_registration_platform.registration.dto.RegistrationRequest;
import com.kashif.event_registration_platform.registration.dto.RegistrationResponse;

public interface RegistrationService {
    RegistrationResponse registerForEvent(Long eventId, RegistrationRequest request, User user);
    RegistrationResponse cancelRegistration (Long registrationId, User user);
}
