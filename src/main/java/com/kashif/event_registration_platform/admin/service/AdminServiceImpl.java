package com.kashif.event_registration_platform.admin.service;

import com.kashif.event_registration_platform.admin.dto.EventRevenue;
import com.kashif.event_registration_platform.admin.dto.RevenueSummaryResponse;
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

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;


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

    @Override
    public RevenueSummaryResponse getRevenueSummary(User organiser){
        List<Event> events = eventRepository.findByOrganiser(organiser);
        List<EventRevenue> eventBreakdown = new ArrayList<>();
        BigDecimal totalRevenue = BigDecimal.ZERO;

        for (Event event : events) {
            long confirmedCount = registrationRepository.countByEventAndStatus(event, RegistrationStatus.CONFIRMED);
            BigDecimal revenue = event.getPrice().multiply(BigDecimal.valueOf(confirmedCount));
            EventRevenue eventRevenue = new EventRevenue(event.getId(), event.getTitle(), revenue);
            eventBreakdown.add(eventRevenue);
            totalRevenue = totalRevenue.add(revenue);
        }
        return new RevenueSummaryResponse(eventBreakdown, totalRevenue);

    }


}
