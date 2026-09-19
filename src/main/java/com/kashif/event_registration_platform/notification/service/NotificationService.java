package com.kashif.event_registration_platform.notification.service;

public interface NotificationService {
    void sendEmail(String recipient, String subject, String body);
    void processRetryQueue();
}
