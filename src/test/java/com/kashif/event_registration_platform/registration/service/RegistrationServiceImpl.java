package com.kashif.event_registration_platform.registration.service;

import com.kashif.event_registration_platform.auth.entity.User;
import com.kashif.event_registration_platform.common.exception.DuplicateResourceException;
import com.kashif.event_registration_platform.common.exception.InvalidStateTransitionException;
import com.kashif.event_registration_platform.common.exception.ResourceNotFoundException;
import com.kashif.event_registration_platform.common.exception.UnauthorizedActionException;
import com.kashif.event_registration_platform.event.entity.Event;
import com.kashif.event_registration_platform.event.repository.EventRepository;
import com.kashif.event_registration_platform.notification.service.NotificationService;
import com.kashif.event_registration_platform.registration.dto.RegistrationRequest;
import com.kashif.event_registration_platform.registration.dto.RegistrationResponse;
import com.kashif.event_registration_platform.registration.entity.Registration;
import com.kashif.event_registration_platform.registration.entity.RegistrationStatus;
import com.kashif.event_registration_platform.registration.entity.TeamRegistration;
import com.kashif.event_registration_platform.registration.repository.RegistrationRepository;
import com.kashif.event_registration_platform.registration.repository.TeamRegistrationRepository;
import com.kashif.event_registration_platform.ticket.service.TicketService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RegistrationServiceImplTest {

    @Mock private RegistrationRepository registrationRepository;
    @Mock private EventRepository eventRepository;
    @Mock private TeamRegistrationRepository teamRegistrationRepository;
    @Mock private TicketService ticketService;
    @Mock private NotificationService notificationService;

    @InjectMocks private RegistrationServiceImpl registrationService;

    private User user(Long id, String email) {
        User user = new User();
        user.setId(id);
        user.setName("User " + id);
        user.setEmail(email);
        return user;
    }

    private Event event(Long id, int capacity) {
        Event event = new Event();
        event.setId(id);
        event.setTitle("Tech Meetup");
        event.setMaxCapacity(capacity);
        return event;
    }

    private Registration registration(Long id, User user, Event event, RegistrationStatus status) {
        Registration r = new Registration();
        r.setId(id);
        r.setUser(user);
        r.setEvent(event);
        r.setStatus(status);
        return r;
    }

    private void stubSaveAssigningId(Long id) {
        when(registrationRepository.save(any(Registration.class))).thenAnswer(inv -> {
            Registration r = inv.getArgument(0);
            r.setId(id);
            return r;
        });
    }


    @Test
    void register_solo_shouldBeConfirmed_andGetTicketAndEmail_whenSeatsAvailable() {
        User user = user(1L, "mark@example.com");
        Event event = event(1L, 10);

        when(eventRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(event));
        when(registrationRepository.countByEventAndStatus(event, RegistrationStatus.CONFIRMED)).thenReturn(0L);
        stubSaveAssigningId(100L);

        RegistrationResponse response = registrationService.registerForEvent(1L, new RegistrationRequest(), user);

        assertEquals(RegistrationStatus.CONFIRMED, response.getStatus());
        assertNull(response.getTeamRegistrationId());
        verify(ticketService).createTicketForRegistration(any(Registration.class));
        verify(notificationService).sendEmail(eq("mark@example.com"), eq("Registration Confirmed"), anyString());
    }

    @Test
    void register_solo_shouldBeWaitlisted_withNoTicket_whenEventIsFull() {
        User user = user(1L, "mark@example.com");
        Event event = event(1L, 2);

        when(eventRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(event));
        when(registrationRepository.countByEventAndStatus(event, RegistrationStatus.CONFIRMED)).thenReturn(2L);
        stubSaveAssigningId(100L);

        RegistrationResponse response = registrationService.registerForEvent(1L, new RegistrationRequest(), user);

        assertEquals(RegistrationStatus.WAITLISTED, response.getStatus());
        verify(ticketService, never()).createTicketForRegistration(any(Registration.class));
        verify(notificationService).sendEmail(eq("mark@example.com"), eq("Added to Waitlist"), anyString());
    }

    @Test
    void register_team_shouldCreateTeamAndOneRegistrationPerMember_withTicketsForEach() {
        User user = user(1L, "lead@example.com");
        Event event = event(1L, 10);
        RegistrationRequest request = new RegistrationRequest();
        request.setTeamSize(3);

        when(eventRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(event));
        when(registrationRepository.countByEventAndStatus(event, RegistrationStatus.CONFIRMED)).thenReturn(0L);
        when(teamRegistrationRepository.save(any(TeamRegistration.class))).thenAnswer(inv -> {
            TeamRegistration t = inv.getArgument(0);
            t.setId(5L);
            return t;
        });
        stubSaveAssigningId(100L);

        RegistrationResponse response = registrationService.registerForEvent(1L, request, user);

        assertEquals(RegistrationStatus.CONFIRMED, response.getStatus());
        assertEquals(Long.valueOf(5L), response.getTeamRegistrationId());
        verify(registrationRepository, times(3)).save(any(Registration.class));
        verify(ticketService, times(3)).createTicketForRegistration(any(Registration.class));
        // one email for the whole team, not one per row
        verify(notificationService, times(1)).sendEmail(eq("lead@example.com"), anyString(), anyString());
    }

    @Test
    void register_shouldThrowDuplicate_whenAlreadyRegistered() {
        User user = user(1L, "mark@example.com");
        Event event = event(1L, 10);

        when(eventRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(event));
        when(registrationRepository.existsByUserAndEventAndStatusIn(eq(user), eq(event), anyList()))
                .thenReturn(true);

        assertThrows(DuplicateResourceException.class,
                () -> registrationService.registerForEvent(1L, new RegistrationRequest(), user));
        verify(registrationRepository, never()).save(any(Registration.class));
    }

    @Test
    void register_shouldReject_whenTeamSizeExceedsEventCapacity() {
        User user = user(1L, "mark@example.com");
        Event event = event(1L, 3);
        RegistrationRequest request = new RegistrationRequest();
        request.setTeamSize(5);

        when(eventRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(event));

        assertThrows(InvalidStateTransitionException.class,
                () -> registrationService.registerForEvent(1L, request, user));
        verify(registrationRepository, never()).save(any(Registration.class));
    }

    @Test
    void register_shouldThrowNotFound_whenEventMissing() {
        when(eventRepository.findByIdForUpdate(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> registrationService.registerForEvent(99L, new RegistrationRequest(), user(1L, "a@example.com")));
    }


    @Test
    void cancel_confirmed_shouldPromoteNextSoloWaitlistedRegistrant() {
        User owner = user(1L, "owner@example.com");
        User waiter = user(2L, "waiter@example.com");
        Event event = event(1L, 1);
        Registration confirmed = registration(1L, owner, event, RegistrationStatus.CONFIRMED);
        Registration waitlisted = registration(2L, waiter, event, RegistrationStatus.WAITLISTED);

        when(registrationRepository.findById(1L)).thenReturn(Optional.of(confirmed));
        when(eventRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(event));
        when(registrationRepository.save(any(Registration.class))).thenAnswer(inv -> inv.getArgument(0));
        when(registrationRepository.countByEventAndStatus(event, RegistrationStatus.CONFIRMED)).thenReturn(0L);
        when(registrationRepository.findByEventAndStatusOrderByRegisteredAtAsc(event, RegistrationStatus.WAITLISTED))
                .thenReturn(List.of(waitlisted));

        RegistrationResponse response = registrationService.cancelRegistration(1L, owner);

        assertEquals(RegistrationStatus.CANCELLED, response.getStatus());
        assertEquals(RegistrationStatus.CONFIRMED, waitlisted.getStatus());
        verify(ticketService).createTicketForRegistration(waitlisted);
        verify(notificationService).sendEmail(eq("waiter@example.com"), anyString(), anyString());
    }

    @Test
    void cancel_confirmed_shouldPromoteWholeWaitlistedTeamTogether_whenItFits() {
        User owner = user(1L, "owner@example.com");
        User lead = user(3L, "lead@example.com");
        Event event = event(1L, 3);
        Registration confirmed = registration(1L, owner, event, RegistrationStatus.CONFIRMED);

        TeamRegistration team = new TeamRegistration();
        team.setId(7L);
        team.setLeadUser(lead);
        team.setEvent(event);
        team.setMemberCount(3);

        Registration t1 = registration(10L, lead, event, RegistrationStatus.WAITLISTED);
        Registration t2 = registration(11L, lead, event, RegistrationStatus.WAITLISTED);
        Registration t3 = registration(12L, lead, event, RegistrationStatus.WAITLISTED);
        t1.setTeamRegistration(team);
        t2.setTeamRegistration(team);
        t3.setTeamRegistration(team);

        when(registrationRepository.findById(1L)).thenReturn(Optional.of(confirmed));
        when(eventRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(event));
        when(registrationRepository.save(any(Registration.class))).thenAnswer(inv -> inv.getArgument(0));
        when(registrationRepository.countByEventAndStatus(event, RegistrationStatus.CONFIRMED)).thenReturn(0L);
        when(registrationRepository.findByEventAndStatusOrderByRegisteredAtAsc(event, RegistrationStatus.WAITLISTED))
                .thenReturn(List.of(t1, t2, t3));

        registrationService.cancelRegistration(1L, owner);

        assertEquals(RegistrationStatus.CONFIRMED, t1.getStatus());
        assertEquals(RegistrationStatus.CONFIRMED, t2.getStatus());
        assertEquals(RegistrationStatus.CONFIRMED, t3.getStatus());
        verify(ticketService, times(3)).createTicketForRegistration(any(Registration.class));
        verify(notificationService, times(1)).sendEmail(eq("lead@example.com"), anyString(), anyString());
    }

    @Test
    void cancel_confirmed_shouldSkipTooLargeWaitlistedTeam_andPromoteSmallerPartyBehindIt() {
        User owner = user(1L, "owner@example.com");
        User lead = user(3L, "lead@example.com");
        User soloUser = user(4L, "solo@example.com");
        Event event = event(1L, 1);
        Registration confirmed = registration(1L, owner, event, RegistrationStatus.CONFIRMED);

        TeamRegistration team = new TeamRegistration();
        team.setId(7L);
        team.setLeadUser(lead);
        team.setEvent(event);
        team.setMemberCount(3); // needs 3 seats, only 1 will be free

        Registration t1 = registration(10L, lead, event, RegistrationStatus.WAITLISTED);
        Registration t2 = registration(11L, lead, event, RegistrationStatus.WAITLISTED);
        Registration t3 = registration(12L, lead, event, RegistrationStatus.WAITLISTED);
        t1.setTeamRegistration(team);
        t2.setTeamRegistration(team);
        t3.setTeamRegistration(team);
        Registration solo = registration(13L, soloUser, event, RegistrationStatus.WAITLISTED);

        when(registrationRepository.findById(1L)).thenReturn(Optional.of(confirmed));
        when(eventRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(event));
        when(registrationRepository.save(any(Registration.class))).thenAnswer(inv -> inv.getArgument(0));
        when(registrationRepository.countByEventAndStatus(event, RegistrationStatus.CONFIRMED)).thenReturn(0L);
        when(registrationRepository.findByEventAndStatusOrderByRegisteredAtAsc(event, RegistrationStatus.WAITLISTED))
                .thenReturn(List.of(t1, t2, t3, solo));

        registrationService.cancelRegistration(1L, owner);

        assertEquals(RegistrationStatus.WAITLISTED, t1.getStatus());
        assertEquals(RegistrationStatus.WAITLISTED, t2.getStatus());
        assertEquals(RegistrationStatus.WAITLISTED, t3.getStatus());
        assertEquals(RegistrationStatus.CONFIRMED, solo.getStatus());
        verify(ticketService, times(1)).createTicketForRegistration(any(Registration.class));
        verify(ticketService).createTicketForRegistration(solo);
        verify(notificationService, never()).sendEmail(eq("lead@example.com"), anyString(), anyString());
        verify(notificationService).sendEmail(eq("solo@example.com"), anyString(), anyString());
    }

    @Test
    void cancel_waitlisted_shouldNotTriggerAnyPromotion() {
        User user = user(1L, "mark@example.com");
        Event event = event(1L, 1);
        Registration waitlisted = registration(2L, user, event, RegistrationStatus.WAITLISTED);

        when(registrationRepository.findById(2L)).thenReturn(Optional.of(waitlisted));
        when(eventRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(event));
        when(registrationRepository.save(any(Registration.class))).thenAnswer(inv -> inv.getArgument(0));

        RegistrationResponse response = registrationService.cancelRegistration(2L, user);

        assertEquals(RegistrationStatus.CANCELLED, response.getStatus());
        verify(registrationRepository, never()).findByEventAndStatusOrderByRegisteredAtAsc(any(), any());
        verify(ticketService, never()).createTicketForRegistration(any());
    }

    @Test
    void cancel_shouldThrowUnauthorized_whenNotTheOwner() {
        User owner = user(1L, "owner@example.com");
        Registration reg = registration(1L, owner, event(1L, 5), RegistrationStatus.CONFIRMED);
        when(registrationRepository.findById(1L)).thenReturn(Optional.of(reg));

        assertThrows(UnauthorizedActionException.class,
                () -> registrationService.cancelRegistration(1L, user(2L, "intruder@example.com")));
        verify(registrationRepository, never()).save(any(Registration.class));
    }

    @Test
    void cancel_shouldThrow_whenAlreadyCancelled() {
        User owner = user(1L, "owner@example.com");
        Registration reg = registration(1L, owner, event(1L, 5), RegistrationStatus.CANCELLED);
        when(registrationRepository.findById(1L)).thenReturn(Optional.of(reg));

        assertThrows(InvalidStateTransitionException.class, () -> registrationService.cancelRegistration(1L, owner));
    }

    @Test
    void cancel_confirmed_shouldCancelItsTicket() {
        User owner = user(1L, "owner@example.com");
        Event event = event(1L, 5);
        Registration confirmed = registration(1L, owner, event, RegistrationStatus.CONFIRMED);

        when(registrationRepository.findById(1L)).thenReturn(Optional.of(confirmed));
        when(eventRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(event));
        when(registrationRepository.save(any(Registration.class))).thenAnswer(inv -> inv.getArgument(0));
        when(registrationRepository.countByEventAndStatus(event, RegistrationStatus.CONFIRMED)).thenReturn(0L);
        when(registrationRepository.findByEventAndStatusOrderByRegisteredAtAsc(event, RegistrationStatus.WAITLISTED))
                .thenReturn(List.of());

        registrationService.cancelRegistration(1L, owner);

        verify(ticketService).cancelTicketForRegistration(confirmed);
    }
}