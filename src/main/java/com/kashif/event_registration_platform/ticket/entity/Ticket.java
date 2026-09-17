package com.kashif.event_registration_platform.ticket.entity;

import com.kashif.event_registration_platform.registration.entity.Registration;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Data
@Table(name ="tickets")
public class Ticket {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "registration_id", nullable = false)
    private Registration registration;

    @Column
    private UUID token;

    @Enumerated(EnumType.STRING)
    @Column
    private TicketStatus status;

    @Column
    private LocalDateTime issuedAt;


}
