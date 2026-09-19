package com.kashif.event_registration_platform.notification.entity;

public enum EmailStatus {
    PENDING,      // logged, waiting for a retry attempt
    SENT,         // successfully delivered
    FAILED        // exhausted retries
}