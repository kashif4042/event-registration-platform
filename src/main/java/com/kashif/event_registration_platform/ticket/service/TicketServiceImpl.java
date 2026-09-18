package com.kashif.event_registration_platform.ticket.service;

import com.kashif.event_registration_platform.auth.entity.User;
import com.kashif.event_registration_platform.common.exception.InvalidStateTransitionException;
import com.kashif.event_registration_platform.common.exception.ResourceNotFoundException;
import com.kashif.event_registration_platform.common.exception.UnauthorizedActionException;
import com.kashif.event_registration_platform.event.entity.EventStatus;
import com.kashif.event_registration_platform.registration.entity.Registration;
import com.kashif.event_registration_platform.ticket.dto.TicketResponse;
import com.kashif.event_registration_platform.ticket.entity.Ticket;
import com.kashif.event_registration_platform.ticket.entity.TicketStatus;
import com.kashif.event_registration_platform.ticket.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TicketServiceImpl implements TicketService {
    private final TicketRepository ticketRepository;

    @Override
    public Ticket createTicketForRegistration(Registration registration){
        Ticket ticket = new Ticket();
        ticket.setRegistration(registration);
        ticket.setToken(UUID.randomUUID());
        ticket.setStatus(TicketStatus.CONFIRMED);
        ticket.setIssuedAt(LocalDateTime.now());

        return ticketRepository.save(ticket);

    }


    @Override
    public TicketResponse checkIn(UUID token, User organiser) {

        Ticket ticket = ticketRepository.findByToken(token)
                .orElseThrow(() -> new ResourceNotFoundException("Token not found"));
        if (!ticket.getRegistration().getEvent().getOrganiser().getId().equals(organiser.getId())) {
            throw new UnauthorizedActionException("You are not authorized to check in attendees for this event");
        }

        if (ticket.getRegistration().getEvent().getStatus() == EventStatus.CANCELLED) {
            throw new InvalidStateTransitionException("Cannot check in - this event has been cancelled");
        }

        if (ticket.getStatus() == TicketStatus.CONFIRMED) {
            ticket.setStatus(TicketStatus.USED);
            ticketRepository.save(ticket);

        } else if (ticket.getStatus() == TicketStatus.USED) {

        } else {
            throw new InvalidStateTransitionException("Ticket is cancelled");
        }

        return new TicketResponse(
                ticket.getId(),
                ticket.getToken(),
                ticket.getStatus(),
                ticket.getIssuedAt()
        );
    }

}
