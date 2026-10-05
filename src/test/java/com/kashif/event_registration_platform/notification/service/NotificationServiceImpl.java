package com.kashif.event_registration_platform.notification.service;

import com.kashif.event_registration_platform.auth.entity.User;
import com.kashif.event_registration_platform.event.entity.Event;
import com.kashif.event_registration_platform.event.repository.EventRepository;
import com.kashif.event_registration_platform.notification.entity.EmailRetryQueue;
import com.kashif.event_registration_platform.notification.entity.EmailStatus;
import com.kashif.event_registration_platform.notification.repository.EmailRetryQueueRepository;
import com.kashif.event_registration_platform.registration.entity.Registration;
import com.kashif.event_registration_platform.registration.repository.RegistrationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

    @Mock private EmailRetryQueueRepository emailRetryQueueRepository;
    @Mock private JavaMailSender mailSender;
    @Mock private EventRepository eventRepository;
    @Mock private RegistrationRepository registrationRepository;

    @InjectMocks private NotificationServiceImpl notificationService;

    private EmailRetryQueue pendingEmail(int attempts) {
        EmailRetryQueue entry = new EmailRetryQueue();
        entry.setRecipient("a@example.com");
        entry.setSubject("Subject");
        entry.setBody("Body");
        entry.setAttempts(attempts);
        entry.setStatus(EmailStatus.PENDING);
        entry.setNextRetryAt(LocalDateTime.now().minusMinutes(1));
        return entry;
    }

    private void stubDueEmails(EmailRetryQueue... entries) {
        when(emailRetryQueueRepository.findByStatusAndNextRetryAtLessThanEqual(
                eq(EmailStatus.PENDING), any(LocalDateTime.class))).thenReturn(List.of(entries));
    }

    // ---------- sendEmail (runs synchronously here: @Async only applies inside a Spring context) ----------

    @Test
    void sendEmail_shouldSendMessage_andLogNothing_whenMailServerWorks() {
        notificationService.sendEmail("a@example.com", "Subject", "Body");

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        assertArrayEquals(new String[]{"a@example.com"}, captor.getValue().getTo());
        assertEquals("Subject", captor.getValue().getSubject());
        verify(emailRetryQueueRepository, never()).save(any(EmailRetryQueue.class));
    }

    @Test
    void sendEmail_shouldLogPendingRetryEntry_whenSendingFails() {
        doThrow(new MailSendException("SMTP down")).when(mailSender).send(any(SimpleMailMessage.class));

        notificationService.sendEmail("a@example.com", "Subject", "Body");

        ArgumentCaptor<EmailRetryQueue> captor = ArgumentCaptor.forClass(EmailRetryQueue.class);
        verify(emailRetryQueueRepository).save(captor.capture());
        EmailRetryQueue saved = captor.getValue();
        assertEquals(EmailStatus.PENDING, saved.getStatus());
        assertEquals(Integer.valueOf(1), saved.getAttempts());
        assertEquals("a@example.com", saved.getRecipient());
        assertNotNull(saved.getNextRetryAt());
    }

    // ---------- processRetryQueue ----------

    @Test
    void processRetryQueue_shouldMarkSent_whenRetrySucceeds() {
        EmailRetryQueue entry = pendingEmail(1);
        stubDueEmails(entry);

        notificationService.processRetryQueue();

        assertEquals(EmailStatus.SENT, entry.getStatus());
        verify(emailRetryQueueRepository).save(entry);
    }

    @Test
    void processRetryQueue_shouldIncrementAttemptsOnSameRow_andStayPending_whenStillFailing() {
        EmailRetryQueue entry = pendingEmail(1);
        stubDueEmails(entry);
        doThrow(new MailSendException("still down")).when(mailSender).send(any(SimpleMailMessage.class));

        notificationService.processRetryQueue();

        assertEquals(Integer.valueOf(2), entry.getAttempts());
        assertEquals(EmailStatus.PENDING, entry.getStatus());
        assertTrue(entry.getNextRetryAt().isAfter(LocalDateTime.now()));
        verify(emailRetryQueueRepository).save(entry);
    }

    @Test
    void processRetryQueue_shouldGiveUpAndMarkFailed_afterThirdFailedAttempt() {
        EmailRetryQueue entry = pendingEmail(2);
        stubDueEmails(entry);
        doThrow(new MailSendException("still down")).when(mailSender).send(any(SimpleMailMessage.class));

        notificationService.processRetryQueue();

        assertEquals(Integer.valueOf(3), entry.getAttempts());
        assertEquals(EmailStatus.FAILED, entry.getStatus());
    }

    // ---------- sendEventReminders ----------

    @Test
    void sendEventReminders_shouldEmailConfirmedRegistrants_andMarkReminderSent() {
        Event event = new Event();
        event.setId(1L);
        event.setTitle("Tech Meetup");
        event.setVenue("Community Hall");

        User u1 = new User();
        u1.setEmail("a@example.com");
        User u2 = new User();
        u2.setEmail("b@example.com");
        Registration r1 = new Registration();
        r1.setUser(u1);
        Registration r2 = new Registration();
        r2.setUser(u2);

        when(eventRepository.findByEventDateBetween(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of(event));
        when(registrationRepository.findByEventAndStatusIn(eq(event), anyList())).thenReturn(List.of(r1, r2));

        notificationService.sendEventReminders();

        verify(mailSender, times(2)).send(any(SimpleMailMessage.class));
        assertTrue(event.getReminderSent());
        verify(eventRepository).save(event);
    }

    @Test
    void sendEventReminders_shouldSkipEventsAlreadyReminded() {
        Event event = new Event();
        event.setId(1L);
        event.setReminderSent(true);

        when(eventRepository.findByEventDateBetween(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of(event));

        notificationService.sendEventReminders();

        verify(registrationRepository, never()).findByEventAndStatusIn(any(), anyList());
        verify(mailSender, never()).send(any(SimpleMailMessage.class));
        verify(eventRepository, never()).save(any(Event.class));
    }
}