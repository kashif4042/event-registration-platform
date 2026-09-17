package com.kashif.event_registration_platform.ticket.repository;

import com.kashif.event_registration_platform.ticket.entity.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface TicketRepository extends JpaRepository<Ticket,Long> {
    Optional<Ticket> findByToken(UUID token);
}
