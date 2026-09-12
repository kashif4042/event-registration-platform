package com.kashif.event_registration_platform.registration.service;

import com.kashif.event_registration_platform.auth.entity.User;
import com.kashif.event_registration_platform.common.exception.DuplicateResourceException;
import com.kashif.event_registration_platform.common.exception.InvalidStateTransitionException;
import com.kashif.event_registration_platform.common.exception.ResourceNotFoundException;
import com.kashif.event_registration_platform.common.exception.UnauthorizedActionException;
import com.kashif.event_registration_platform.event.entity.Event;
import com.kashif.event_registration_platform.event.repository.EventRepository;
import com.kashif.event_registration_platform.registration.dto.RegistrationRequest;
import com.kashif.event_registration_platform.registration.dto.RegistrationResponse;
import com.kashif.event_registration_platform.registration.entity.Registration;
import com.kashif.event_registration_platform.registration.entity.RegistrationStatus;
import com.kashif.event_registration_platform.registration.entity.TeamRegistration;
import com.kashif.event_registration_platform.registration.repository.RegistrationRepository;
import com.kashif.event_registration_platform.registration.repository.TeamRegistrationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RegistrationServiceImpl implements RegistrationService {
    private final RegistrationRepository registrationRepository;
    private final EventRepository eventRepository;
    private final TeamRegistrationRepository teamRegistrationRepository;

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

        Registration leadRegistration;

        if (seatsNeeded > 1) {
            // Team registration
            TeamRegistration teamRegistration = new TeamRegistration();
            teamRegistration.setLeadUser(user);
            teamRegistration.setEvent(event);
            teamRegistration.setMemberCount(seatsNeeded);
            teamRegistration.setGroupToken(UUID.randomUUID());
            TeamRegistration savedTeamRegistration = teamRegistrationRepository.save(teamRegistration);

            Registration firstRegistration = null;
            for (int i = 0; i < seatsNeeded; i++) {
                Registration registration = new Registration();
                registration.setUser(user);
                registration.setEvent(event);
                registration.setStatus(status);
                registration.setRegisteredAt(LocalDateTime.now());
                registration.setTeamRegistration(savedTeamRegistration);
                Registration saved = registrationRepository.save(registration);
                if (i == 0) {
                    firstRegistration = saved;
                }
            }
            leadRegistration = firstRegistration;

        } else {
            // Solo registration
            Registration registration = new Registration();
            registration.setUser(user);
            registration.setEvent(event);
            registration.setStatus(status);
            registration.setRegisteredAt(LocalDateTime.now());
            leadRegistration = registrationRepository.save(registration);
        }

        return new RegistrationResponse(
                leadRegistration.getId(),
                leadRegistration.getEvent().getId(),
                leadRegistration.getStatus(),
                leadRegistration.getRegisteredAt(),
                leadRegistration.getTeamRegistration() != null ? leadRegistration.getTeamRegistration().getId() : null
        );
    }

    private void promoteWaitlistedIfPossible(Event event, long availableSeats) {
        List<Registration> waitlisted = registrationRepository
                .findByEventAndStatusOrderByRegisteredAtAsc(event, RegistrationStatus.WAITLISTED);

        Set<Long> processedTeamIds = new HashSet<>();

        for (Registration reg : waitlisted) {
            if (availableSeats <= 0) {
                break; // no more capacity to give away
            }

            TeamRegistration team = reg.getTeamRegistration();

            if (team != null) {
                // Skip if we've already processed this team (from an earlier row in the loop)
                if (processedTeamIds.contains(team.getId())) {
                    continue;
                }
                int groupSize = team.getMemberCount();
                if (groupSize <= availableSeats) {
                    // Promote every registration row belonging to this team
                    List<Registration> teamMembers = waitlisted.stream()
                            .filter(r -> r.getTeamRegistration() != null && r.getTeamRegistration().getId().equals(team.getId()))
                            .collect(Collectors.toList());
                    for (Registration member : teamMembers) {
                        member.setStatus(RegistrationStatus.CONFIRMED);
                        registrationRepository.save(member);
                    }
                    availableSeats -= groupSize;
                }
                processedTeamIds.add(team.getId()); // mark as processed whether promoted or skipped
            } else {
                // Solo registration, group size is 1
                if (1 <= availableSeats) {
                    reg.setStatus(RegistrationStatus.CONFIRMED);
                    registrationRepository.save(reg);
                    availableSeats -= 1;
                }
            }
        }
    }

    @Override
    @Transactional
    public RegistrationResponse cancelRegistration(Long registrationId, User user){
        Registration registration = registrationRepository.findById(registrationId)
                .orElseThrow(() -> new ResourceNotFoundException("Registration not found"));

        if (!registration.getUser().getId().equals(user.getId())) {
            throw new UnauthorizedActionException("You are not authorized to cancel this registration");
        }

        if (registration.getStatus() == RegistrationStatus.CANCELLED) {
            throw new InvalidStateTransitionException("This registration is already cancelled");
        }

// Lock the event row before touching capacity-related logic
        Event event = eventRepository.findByIdForUpdate(registration.getEvent().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Event not found"));

        RegistrationStatus previousStatus = registration.getStatus();
        registration.setStatus(RegistrationStatus.CANCELLED);
        Registration savedRegistration = registrationRepository.save(registration);

        if (previousStatus == RegistrationStatus.CONFIRMED) {
            long availableSeats = event.getMaxCapacity() - registrationRepository.countByEventAndStatus(event, RegistrationStatus.CONFIRMED);
            promoteWaitlistedIfPossible(event, availableSeats);
        }

        return new RegistrationResponse(
                savedRegistration.getId(),
                savedRegistration.getEvent().getId(),
                savedRegistration.getStatus(),
                savedRegistration.getRegisteredAt(),
                savedRegistration.getTeamRegistration() != null ? savedRegistration.getTeamRegistration().getId() : null
        );



    }


}
