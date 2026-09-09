package com.kashif.event_registration_platform.registration.entity;

import com.kashif.event_registration_platform.auth.entity.User;
import com.kashif.event_registration_platform.event.entity.Event;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Data
@Table(name = "registrations")
public class Registration {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @JoinColumn(name = "event_id", nullable = false)
    @ManyToOne
    private Event event;

    @JoinColumn(name = "team_registration_id", nullable = true)
    @ManyToOne
    private TeamRegistration teamRegistration;

    @Enumerated(EnumType.STRING)
    @Column
    private RegistrationStatus status;

    @Column(name = "registered_at")
    private LocalDateTime registeredAt;


}
