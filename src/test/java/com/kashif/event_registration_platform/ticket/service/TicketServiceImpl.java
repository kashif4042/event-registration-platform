package com.kashif.event_registration_platform.ticket.service;

import com.kashif.event_registration_platform.auth.entity.User;
import com.kashif.event_registration_platform.common.exception.InvalidStateTransitionException;
import com.kashif.event_registration_platform.common.exception.ResourceNotFoundException;
import com.kashif.event_registration_platform.common.exception.UnauthorizedActionException;
import com.kashif.event_registration_platform.event.entity.Event;
import com.kashif.event_registration_platform.event.entity.EventStatus;
import com.kashif.event_registration_platform.registration.entity.Registration;
import com.kashif.event_registration_platform.ticket.dto.TicketResponse;
import com.kashif.event_registration_platform.ticket.entity.Ticket;
import com.kashif.event_registration_platform.ticket.entity.TicketStatus;
import com.kashif.event_registration_platform.ticket.repository.TicketRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TicketServiceImplTest {

    @Mock private TicketRepository ticketRepository;

    @InjectMocks private TicketServiceImpl ticketService;

    private User user(Long id) {
        User user = new User();
        user.setId(id);
        return user;
    }

    private Ticket ticket(TicketStatus ticketStatus, Long organiserId, EventStatus eventStatus) {
        Event event = new Event();
        event.setId(1L);
        event.setOrganiser(user(organiserId));
        event.setStatus(eventStatus);

        Registration registration = new Registration();
        registration.setId(1L);
        registration.setEvent(event);

        Ticket ticket = new Ticket();
        ticket.setId(1L);
        ticket.setToken(UUID.randomUUID());
        ticket.setStatus(ticketStatus);
        ticket.setRegistration(registration);
        return ticket;
    }

    // ---------- createTicketForRegistration ----------

    @Test
    void createTicket_shouldIssueConfirmedTicketWithUniqueToken() {
        Registration registration = new Registration();
        registration.setId(1L);
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(inv -> inv.getArgument(0));

        Ticket ticket = ticketService.createTicketForRegistration(registration);

        assertNotNull(ticket.getToken());
        assertEquals(TicketStatus.CONFIRMED, ticket.getStatus());
        assertNotNull(ticket.getIssuedAt());
        assertSame(registration, ticket.getRegistration());
    }

    // ---------- checkIn ----------

    @Test
    void checkIn_shouldMarkTicketUsed_onFirstScan() {
        Ticket ticket = ticket(TicketStatus.CONFIRMED, 1L, EventStatus.PUBLISHED);
        when(ticketRepository.findByToken(ticket.getToken())).thenReturn(Optional.of(ticket));

        TicketResponse response = ticketService.checkIn(ticket.getToken(), user(1L));

        assertEquals(TicketStatus.USED, response.getStatus());
        verify(ticketRepository).save(ticket);
    }

    @Test
    void checkIn_shouldBeIdempotent_onRepeatScan() {
        Ticket ticket = ticket(TicketStatus.USED, 1L, EventStatus.PUBLISHED);
        when(ticketRepository.findByToken(ticket.getToken())).thenReturn(Optional.of(ticket));

        TicketResponse response = assertDoesNotThrow(() -> ticketService.checkIn(ticket.getToken(), user(1L)));

        assertEquals(TicketStatus.USED, response.getStatus());
        verify(ticketRepository, never()).save(any(Ticket.class));
    }

    @Test
    void checkIn_shouldThrow_whenTicketIsCancelled() {
        Ticket ticket = ticket(TicketStatus.CANCELLED, 1L, EventStatus.PUBLISHED);
        when(ticketRepository.findByToken(ticket.getToken())).thenReturn(Optional.of(ticket));

        assertThrows(InvalidStateTransitionException.class, () -> ticketService.checkIn(ticket.getToken(), user(1L)));
    }

    @Test
    void checkIn_shouldThrowUnauthorized_whenCallerIsNotTheEventOrganiser() {
        Ticket ticket = ticket(TicketStatus.CONFIRMED, 1L, EventStatus.PUBLISHED);
        when(ticketRepository.findByToken(ticket.getToken())).thenReturn(Optional.of(ticket));

        assertThrows(UnauthorizedActionException.class, () -> ticketService.checkIn(ticket.getToken(), user(2L)));
        verify(ticketRepository, never()).save(any(Ticket.class));
    }

    @Test
    void checkIn_shouldThrow_whenEventIsCancelled() {
        Ticket ticket = ticket(TicketStatus.CONFIRMED, 1L, EventStatus.CANCELLED);
        when(ticketRepository.findByToken(ticket.getToken())).thenReturn(Optional.of(ticket));

        assertThrows(InvalidStateTransitionException.class, () -> ticketService.checkIn(ticket.getToken(), user(1L)));
        verify(ticketRepository, never()).save(any(Ticket.class));
    }

    @Test
    void checkIn_shouldThrowNotFound_whenTokenUnknown() {
        UUID token = UUID.randomUUID();
        when(ticketRepository.findByToken(token)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> ticketService.checkIn(token, user(1L)));
    }
}