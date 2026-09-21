package com.kashif.event_registration_platform.notification.service;

import com.kashif.event_registration_platform.event.entity.Event;
import com.kashif.event_registration_platform.event.repository.EventRepository;
import com.kashif.event_registration_platform.notification.entity.EmailRetryQueue;
import com.kashif.event_registration_platform.notification.entity.EmailStatus;
import com.kashif.event_registration_platform.notification.repository.EmailRetryQueueRepository;
import com.kashif.event_registration_platform.registration.entity.Registration;
import com.kashif.event_registration_platform.registration.entity.RegistrationStatus;
import com.kashif.event_registration_platform.registration.repository.RegistrationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;



@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService{
    private final EmailRetryQueueRepository emailRetryQueueRepository;
    private final JavaMailSender mailSender;
    private final EventRepository eventRepository;
    private final RegistrationRepository registrationRepository;

    @Async
    @Override
    public void sendEmail(String recipient, String subject, String body) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(recipient);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
        } catch (Exception e) {
            // Sending failed
            EmailRetryQueue retryEntry = new EmailRetryQueue();
            retryEntry.setRecipient(recipient);
            retryEntry.setSubject(subject);
            retryEntry.setBody(body);
            retryEntry.setAttempts(1);
            retryEntry.setNextRetryAt(LocalDateTime.now().plusMinutes(5));
            retryEntry.setStatus(EmailStatus.PENDING);
            emailRetryQueueRepository.save(retryEntry);
        }
    }

    @Scheduled(fixedRate = 300000)
    @Override
    public void processRetryQueue() {
        List<EmailRetryQueue> dueEmails = emailRetryQueueRepository
                .findByStatusAndNextRetryAtLessThanEqual(EmailStatus.PENDING, LocalDateTime.now());

        for (EmailRetryQueue emailEntry : dueEmails) {
            try {
                SimpleMailMessage message = new SimpleMailMessage();
                message.setTo(emailEntry.getRecipient());
                message.setSubject(emailEntry.getSubject());
                message.setText(emailEntry.getBody());
                mailSender.send(message);

                // Success this time — mark as SENT
                emailEntry.setStatus(EmailStatus.SENT);
                emailRetryQueueRepository.save(emailEntry);

            } catch (Exception e) {
                // Still failing — increment attempts on the SAME row
                int newAttempts = emailEntry.getAttempts() + 1;
                emailEntry.setAttempts(newAttempts);

                if (newAttempts >= 3) {
                    emailEntry.setStatus(EmailStatus.FAILED);
                } else {
                    emailEntry.setNextRetryAt(LocalDateTime.now().plusMinutes(10));
                    // status stays PENDING
                }
                emailRetryQueueRepository.save(emailEntry);
            }
        }
    }
    @Scheduled(cron = "0 0 * * * *")
    @Override
    public void sendEventReminders(){
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime in24hrs = LocalDateTime.now().plusHours(24);

        List<Event> upcomingEvents = eventRepository.findByEventDateBetween(now,in24hrs);

        for (Event event : upcomingEvents) {
            if (Boolean.TRUE.equals(event.getReminderSent())) {
                continue;
            }
            List<Registration> confirmedRegistration =
                    registrationRepository.findByEventAndStatusIn(event, List.of(RegistrationStatus.CONFIRMED));
            for (Registration reg : confirmedRegistration) {
                sendEmail(
                        reg.getUser().getEmail(),
                        "Reminder: " + event.getTitle() + " is coming up!",
                        "This is a reminder that \"" + event.getTitle() + "\" starts within 24 hours at " + event.getVenue() + "."
                );
            }
            event.setReminderSent(true);
            eventRepository.save(event);
        }
    }

}
