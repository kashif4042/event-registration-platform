package com.kashif.event_registration_platform.admin.service;

import com.kashif.event_registration_platform.admin.dto.StatsResponse;
import com.kashif.event_registration_platform.auth.entity.User;
import com.kashif.event_registration_platform.common.exception.ResourceNotFoundException;
import com.kashif.event_registration_platform.common.exception.UnauthorizedActionException;
import com.kashif.event_registration_platform.event.entity.Event;
import com.kashif.event_registration_platform.event.repository.EventRepository;
import com.kashif.event_registration_platform.registration.entity.RegistrationStatus;
import com.kashif.event_registration_platform.registration.repository.RegistrationRepository;
import com.kashif.event_registration_platform.ticket.entity.TicketStatus;
import com.kashif.event_registration_platform.ticket.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService{
    private final EventRepository eventRepository;
    private final TicketRepository ticketRepository;
    private final RegistrationRepository registrationRepository;

    @Override
    public StatsResponse getEventStats(Long eventId, User organiser){
        Event event = eventRepository.findById(eventId)
                .orElseThrow(()-> new ResourceNotFoundException("Event Not Found"));
        if(!event.getOrganiser().getId().equals(organiser.getId())){
            throw new UnauthorizedActionException("You are not authorized to modify this event");
        }
        long confirmedRegistration = registrationRepository.
                countByEventAndStatus(event, RegistrationStatus.CONFIRMED);
        long waitlistedRegistration = registrationRepository.
                countByEventAndStatus(event, RegistrationStatus.WAITLISTED);
        long totalCheckedIn = ticketRepository.
                countByRegistration_EventAndStatus(event, TicketStatus.USED);
        double checkInRate = (confirmedRegistration == 0) ? 0.0 : (double) totalCheckedIn / confirmedRegistration;

        return new StatsResponse(
                event.getId(),
                event.getTitle(),
                confirmedRegistration,
                waitlistedRegistration,
                totalCheckedIn,
                checkInRate
        );


    }


}
