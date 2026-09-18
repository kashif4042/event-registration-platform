package com.kashif.event_registration_platform.ticket.service;

import com.kashif.event_registration_platform.auth.entity.User;
import com.kashif.event_registration_platform.registration.entity.Registration;
import com.kashif.event_registration_platform.ticket.dto.TicketResponse;
import com.kashif.event_registration_platform.ticket.entity.Ticket;

import java.util.UUID;

public interface TicketService {
    Ticket createTicketForRegistration(Registration registration);
    TicketResponse checkIn(UUID token, User organiser);

}
