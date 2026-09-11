package com.kashif.event_registration_platform.registration.service;

import com.kashif.event_registration_platform.auth.entity.User;
import com.kashif.event_registration_platform.common.exception.DuplicateResourceException;
import com.kashif.event_registration_platform.common.exception.InvalidStateTransitionException;
import com.kashif.event_registration_platform.common.exception.ResourceNotFoundException;
import com.kashif.event_registration_platform.event.entity.Event;
import com.kashif.event_registration_platform.event.repository.EventRepository;
import com.kashif.event_registration_platform.registration.dto.RegistrationRequest;
import com.kashif.event_registration_platform.registration.dto.RegistrationResponse;
import com.kashif.event_registration_platform.registration.entity.Registration;
import com.kashif.event_registration_platform.registration.entity.RegistrationStatus;
import com.kashif.event_registration_platform.registration.repository.RegistrationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RegistrationServiceImpl implements RegistrationService {
    private final RegistrationRepository registrationRepository;
    private final EventRepository eventRepository;

    @Override
    @Transactional
    public RegistrationResponse registerForEvent(Long eventId,RegistrationRequest request, User user){
        Event event = eventRepository.findByIdForUpdate(eventId)
                .orElseThrow(()-> new ResourceNotFoundException("Event Not Found"));
        boolean alreadyRegistered = registrationRepository.existsByUserAndEventAndStatusIn(
                user,event, List.of(RegistrationStatus.CONFIRMED, RegistrationStatus.WAITLISTED)
        );
        if(alreadyRegistered){
            throw new DuplicateResourceException("You are already registered for this event");
        }
        int seatsNeeded = (request.getTeamSize() != null) ? request.getTeamSize() : 1;

        if (seatsNeeded > event.getMaxCapacity()) {
            throw new InvalidStateTransitionException("Requested team size exceeds this event's maximum capacity");
        }

        long confirmedCount = registrationRepository.countByEventAndStatus(event, RegistrationStatus.CONFIRMED);

        RegistrationStatus status;
        if (confirmedCount + seatsNeeded <= event.getMaxCapacity()) {
            status = RegistrationStatus.CONFIRMED;
        } else {
            status = RegistrationStatus.WAITLISTED;
        }

        Registration registration = new Registration();
        registration.setUser(user);
        registration.setEvent(event);
        registration.setStatus(status);
        registration.setRegisteredAt(LocalDateTime.now());

        Registration savedRegistration = registrationRepository.save(registration);
        return new RegistrationResponse(
                savedRegistration.getId(),
                savedRegistration.getEvent().getId(),
                savedRegistration.getStatus(),
                savedRegistration.getRegisteredAt(),
                savedRegistration.getTeamRegistration() != null ? savedRegistration.getTeamRegistration().getId() : null
        );
    }


}
