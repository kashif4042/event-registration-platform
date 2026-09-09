package com.kashif.event_registration_platform.registration.entity;

import com.kashif.event_registration_platform.auth.entity.User;
import com.kashif.event_registration_platform.event.entity.Event;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Data
@Table(name = "team_registrations")
public class TeamRegistration {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "lead_user_id", nullable = false)
    private User leadUser;

    @JoinColumn(name = "event_id", nullable = false)
    @ManyToOne
    private Event event;

    @Column(name = "member_count")
    private Integer memberCount;

    @Column
    private UUID groupToken;
}

