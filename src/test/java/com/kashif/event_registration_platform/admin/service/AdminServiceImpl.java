package com.kashif.event_registration_platform.admin.service;

import com.kashif.event_registration_platform.admin.dto.RevenueSummaryResponse;
import com.kashif.event_registration_platform.admin.dto.StatsResponse;
import com.kashif.event_registration_platform.auth.entity.User;
import com.kashif.event_registration_platform.common.exception.InvalidStateTransitionException;
import com.kashif.event_registration_platform.common.exception.ResourceNotFoundException;
import com.kashif.event_registration_platform.common.exception.UnauthorizedActionException;
import com.kashif.event_registration_platform.event.dto.EventResponse;
import com.kashif.event_registration_platform.event.entity.Event;
import com.kashif.event_registration_platform.event.entity.EventStatus;
import com.kashif.event_registration_platform.event.repository.EventRepository;
import com.kashif.event_registration_platform.notification.service.NotificationService;
import com.kashif.event_registration_platform.registration.entity.Registration;
import com.kashif.event_registration_platform.registration.entity.RegistrationStatus;
import com.kashif.event_registration_platform.registration.repository.RegistrationRepository;
import com.kashif.event_registration_platform.ticket.entity.TicketStatus;
import com.kashif.event_registration_platform.ticket.repository.TicketRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminServiceImplTest {

    @Mock private EventRepository eventRepository;
    @Mock private TicketRepository ticketRepository;
    @Mock private RegistrationRepository registrationRepository;
    @Mock private NotificationService notificationService;

    @InjectMocks private AdminServiceImpl adminService;

    private User user(Long id, String email) {
        User user = new User();
        user.setId(id);
        user.setName("User " + id);
        user.setEmail(email);
        return user;
    }

    private Event event(Long id, User organiser, EventStatus status, String price) {
        Event event = new Event();
        event.setId(id);
        event.setTitle("Event " + id);
        event.setOrganiser(organiser);
        event.setStatus(status);
        event.setPrice(new BigDecimal(price));
        return event;
    }

    // ---------- getEventStats ----------

    @Test
    void getEventStats_shouldComputeCounts_andCheckInRate() {
        User organiser = user(1L, "org@example.com");
        Event event = event(1L, organiser, EventStatus.PUBLISHED, "100.00");

        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(registrationRepository.countByEventAndStatus(event, RegistrationStatus.CONFIRMED)).thenReturn(4L);
        when(registrationRepository.countByEventAndStatus(event, RegistrationStatus.WAITLISTED)).thenReturn(2L);
        when(ticketRepository.countByRegistration_EventAndStatus(event, TicketStatus.USED)).thenReturn(3L);

        StatsResponse stats = adminService.getEventStats(1L, organiser);

        assertEquals(4L, stats.getTotalConfirmed());
        assertEquals(2L, stats.getTotalWaitlisted());
        assertEquals(3L, stats.getTotalCheckedIn());
        assertEquals(0.75, stats.getCheckInRate(), 0.0001);
    }

    @Test
    void getEventStats_shouldReturnZeroRate_whenNobodyConfirmed() {
        User organiser = user(1L, "org@example.com");
        Event event = event(1L, organiser, EventStatus.PUBLISHED, "100.00");

        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(registrationRepository.countByEventAndStatus(event, RegistrationStatus.CONFIRMED)).thenReturn(0L);
        when(registrationRepository.countByEventAndStatus(event, RegistrationStatus.WAITLISTED)).thenReturn(0L);
        when(ticketRepository.countByRegistration_EventAndStatus(event, TicketStatus.USED)).thenReturn(0L);

        StatsResponse stats = adminService.getEventStats(1L, organiser);

        assertEquals(0.0, stats.getCheckInRate(), 0.0001); // no divide-by-zero
    }

    @Test
    void getEventStats_shouldThrowUnauthorized_whenNotTheOrganiser() {
        Event event = event(1L, user(1L, "org@example.com"), EventStatus.PUBLISHED, "100.00");
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));

        assertThrows(UnauthorizedActionException.class,
                () -> adminService.getEventStats(1L, user(2L, "other@example.com")));
    }

    // ---------- getRevenueSummary ----------

    @Test
    void getRevenueSummary_shouldMultiplyPriceByConfirmedCount_andSumTotal() {
        User organiser = user(1L, "org@example.com");
        Event e1 = event(1L, organiser, EventStatus.PUBLISHED, "100.00");
        Event e2 = event(2L, organiser, EventStatus.PUBLISHED, "50.00");

        when(eventRepository.findByOrganiser(organiser)).thenReturn(List.of(e1, e2));
        when(registrationRepository.countByEventAndStatus(e1, RegistrationStatus.CONFIRMED)).thenReturn(3L);
        when(registrationRepository.countByEventAndStatus(e2, RegistrationStatus.CONFIRMED)).thenReturn(2L);

        RevenueSummaryResponse summary = adminService.getRevenueSummary(organiser);

        assertEquals(2, summary.getEventBreakdown().size());
        assertEquals(0, new BigDecimal("300").compareTo(summary.getEventBreakdown().get(0).getRevenue()));
        assertEquals(0, new BigDecimal("100").compareTo(summary.getEventBreakdown().get(1).getRevenue()));
        assertEquals(0, new BigDecimal("400").compareTo(summary.getTotalRevenue()));
    }

    // ---------- bulkCancelEvent ----------

    @Test
    void bulkCancelEvent_shouldCancelAnyEvent_withoutOwnershipCheck_andNotifyRegistrants() {
        User organiser = user(1L, "org@example.com");
        User admin = user(99L, "admin@example.com"); // not the owner - that's the point
        Event event = event(5L, organiser, EventStatus.PUBLISHED, "100.00");

        Registration r1 = new Registration();
        r1.setUser(user(2L, "a@example.com"));
        Registration r2 = new Registration();
        r2.setUser(user(3L, "b@example.com"));

        when(eventRepository.findById(5L)).thenReturn(Optional.of(event));
        when(registrationRepository.findByEventAndStatusIn(any(Event.class), anyList()))
                .thenReturn(List.of(r1, r2));
        when(eventRepository.save(any(Event.class))).thenAnswer(inv -> inv.getArgument(0));

        EventResponse response = adminService.bulkCancelEvent(5L, admin);

        assertEquals(EventStatus.CANCELLED, response.getStatus());
        verify(notificationService, times(2)).sendEmail(anyString(), contains("Event Cancelled"), anyString());
    }

    @Test
    void bulkCancelEvent_shouldThrow_whenEventAlreadyCancelled() {
        Event event = event(5L, user(1L, "org@example.com"), EventStatus.CANCELLED, "100.00");
        when(eventRepository.findById(5L)).thenReturn(Optional.of(event));

        assertThrows(InvalidStateTransitionException.class,
                () -> adminService.bulkCancelEvent(5L, user(99L, "admin@example.com")));
        verify(notificationService, never()).sendEmail(anyString(), anyString(), anyString());
    }

    @Test
    void bulkCancelEvent_shouldThrowNotFound_whenEventMissing() {
        when(eventRepository.findById(404L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> adminService.bulkCancelEvent(404L, user(99L, "admin@example.com")));
    }
}