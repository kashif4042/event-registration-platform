package com.kashif.event_registration_platform.event.service;

import com.kashif.event_registration_platform.auth.entity.User;
import com.kashif.event_registration_platform.common.exception.InvalidStateTransitionException;
import com.kashif.event_registration_platform.common.exception.ResourceNotFoundException;
import com.kashif.event_registration_platform.common.exception.UnauthorizedActionException;
import com.kashif.event_registration_platform.event.dto.EventRequest;
import com.kashif.event_registration_platform.event.dto.EventResponse;
import com.kashif.event_registration_platform.event.entity.Event;
import com.kashif.event_registration_platform.event.entity.EventStatus;
import com.kashif.event_registration_platform.event.repository.EventRepository;
import com.kashif.event_registration_platform.notification.service.NotificationService;
import com.kashif.event_registration_platform.registration.entity.Registration;
import com.kashif.event_registration_platform.registration.entity.RegistrationStatus;
import com.kashif.event_registration_platform.registration.repository.RegistrationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventServiceImplTest {

    @Mock private EventRepository eventRepository;
    @Mock private NotificationService notificationService;
    @Mock private RegistrationRepository registrationRepository;

    @InjectMocks private EventServiceImpl eventService;

    private User user(Long id, String email) {
        User user = new User();
        user.setId(id);
        user.setName("User " + id);
        user.setEmail(email);
        return user;
    }

    private Event event(Long id, User organiser, EventStatus status) {
        Event event = new Event();
        event.setId(id);
        event.setTitle("Tech Meetup");
        event.setVenue("Community Hall");
        event.setEventDate(LocalDateTime.now().plusDays(10));
        event.setMaxCapacity(100);
        event.setPrice(new BigDecimal("150.00"));
        event.setOrganiser(organiser);
        event.setStatus(status);
        event.setCreatedAt(LocalDateTime.now());
        return event;
    }


    @Test
    void createEvent_shouldCreateDraftEventOwnedByOrganiser() {
        User organiser = user(1L, "org@example.com");
        EventRequest request = new EventRequest();
        request.setTitle("Tech Meetup");
        request.setVenue("Community Hall");
        request.setEventDate(LocalDateTime.now().plusDays(10));
        request.setMaxCapacity(100);
        request.setPrice(new BigDecimal("150.00"));

        when(eventRepository.save(any(Event.class))).thenAnswer(inv -> {
            Event e = inv.getArgument(0);
            e.setId(1L);
            return e;
        });

        EventResponse response = eventService.createEvent(request, organiser);

        assertEquals(EventStatus.DRAFT, response.getStatus());
        assertEquals("Tech Meetup", response.getTitle());
        assertEquals("User 1", response.getOrganiserName());
        verify(eventRepository).save(any(Event.class));
    }


    @Test
    void publishEvent_shouldPublish_whenOwnerAndDraft() {
        User organiser = user(1L, "org@example.com");
        Event event = event(5L, organiser, EventStatus.DRAFT);

        when(eventRepository.findById(5L)).thenReturn(Optional.of(event));
        when(eventRepository.save(any(Event.class))).thenAnswer(inv -> inv.getArgument(0));

        EventResponse response = eventService.publishEvent(5L, organiser);

        assertEquals(EventStatus.PUBLISHED, response.getStatus());
    }

    @Test
    void publishEvent_shouldThrowUnauthorized_whenNotOwner() {
        Event event = event(5L, user(1L, "org@example.com"), EventStatus.DRAFT);
        when(eventRepository.findById(5L)).thenReturn(Optional.of(event));

        assertThrows(UnauthorizedActionException.class,
                () -> eventService.publishEvent(5L, user(2L, "other@example.com")));
        verify(eventRepository, never()).save(any(Event.class));
    }

    @Test
    void publishEvent_shouldThrowInvalidState_whenAlreadyPublished() {
        User organiser = user(1L, "org@example.com");
        Event event = event(5L, organiser, EventStatus.PUBLISHED);
        when(eventRepository.findById(5L)).thenReturn(Optional.of(event));

        assertThrows(InvalidStateTransitionException.class, () -> eventService.publishEvent(5L, organiser));
        verify(eventRepository, never()).save(any(Event.class));
    }

    @Test
    void publishEvent_shouldThrowNotFound_whenEventMissing() {
        when(eventRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> eventService.publishEvent(99L, user(1L, "org@example.com")));
    }


    @Test
    void unpublishEvent_shouldReturnToDraft_whenPublished() {
        User organiser = user(1L, "org@example.com");
        Event event = event(5L, organiser, EventStatus.PUBLISHED);

        when(eventRepository.findById(5L)).thenReturn(Optional.of(event));
        when(eventRepository.save(any(Event.class))).thenAnswer(inv -> inv.getArgument(0));

        EventResponse response = eventService.unpublishEvent(5L, organiser);

        assertEquals(EventStatus.DRAFT, response.getStatus());
    }

    @Test
    void unpublishEvent_shouldThrow_whenEventIsStillDraft() {
        User organiser = user(1L, "org@example.com");
        when(eventRepository.findById(5L)).thenReturn(Optional.of(event(5L, organiser, EventStatus.DRAFT)));

        assertThrows(InvalidStateTransitionException.class, () -> eventService.unpublishEvent(5L, organiser));
    }


    @Test
    void cancelEvent_shouldCancelAndNotifyEveryActiveRegistrant() {
        User organiser = user(1L, "org@example.com");
        Event event = event(5L, organiser, EventStatus.PUBLISHED);

        Registration r1 = new Registration();
        r1.setUser(user(2L, "a@example.com"));
        r1.setStatus(RegistrationStatus.CONFIRMED);
        Registration r2 = new Registration();
        r2.setUser(user(3L, "b@example.com"));
        r2.setStatus(RegistrationStatus.WAITLISTED);

        when(eventRepository.findById(5L)).thenReturn(Optional.of(event));
        when(eventRepository.save(any(Event.class))).thenAnswer(inv -> inv.getArgument(0));
        when(registrationRepository.findByEventAndStatusIn(any(Event.class), anyList()))
                .thenReturn(List.of(r1, r2));

        EventResponse response = eventService.cancelEvent(5L, organiser);

        assertEquals(EventStatus.CANCELLED, response.getStatus());
        verify(notificationService).sendEmail(eq("a@example.com"), contains("Event Cancelled"), anyString());
        verify(notificationService).sendEmail(eq("b@example.com"), contains("Event Cancelled"), anyString());
    }

    @Test
    void cancelEvent_shouldThrow_whenEventAlreadyCompleted() {
        User organiser = user(1L, "org@example.com");
        when(eventRepository.findById(5L)).thenReturn(Optional.of(event(5L, organiser, EventStatus.COMPLETED)));

        assertThrows(InvalidStateTransitionException.class, () -> eventService.cancelEvent(5L, organiser));
        verify(notificationService, never()).sendEmail(anyString(), anyString(), anyString());
    }

    @Test
    void cancelEvent_shouldThrowUnauthorized_whenNotOwner() {
        Event event = event(5L, user(1L, "org@example.com"), EventStatus.PUBLISHED);
        when(eventRepository.findById(5L)).thenReturn(Optional.of(event));

        assertThrows(UnauthorizedActionException.class,
                () -> eventService.cancelEvent(5L, user(2L, "other@example.com")));
        verify(notificationService, never()).sendEmail(anyString(), anyString(), anyString());
    }


    @Test
    void getPublishedEvents_shouldMapEveryPublishedEvent() {
        User organiser = user(1L, "org@example.com");
        when(eventRepository.findByStatus(EventStatus.PUBLISHED)).thenReturn(List.of(
                event(1L, organiser, EventStatus.PUBLISHED),
                event(2L, organiser, EventStatus.PUBLISHED)));

        List<EventResponse> responses = eventService.getPublishedEvents();

        assertEquals(2, responses.size());
        assertEquals(EventStatus.PUBLISHED, responses.get(0).getStatus());
    }
}