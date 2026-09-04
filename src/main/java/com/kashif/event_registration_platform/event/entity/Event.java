package com.kashif.event_registration_platform.event.entity;

import com.kashif.event_registration_platform.auth.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;


@Entity
@AllArgsConstructor
@NoArgsConstructor
@Data
@Table(name = "events")
public class Event {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "organiser_id")
    private User organiser;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "venue", nullable = false)
    private String venue;

    @Column(name = "event_date", nullable = false)
    private LocalDateTime eventDate;

    @Column(name = "max_capacity", nullable = false)
    private Integer maxCapacity;

    @Column(name = "price", nullable = false)
    private BigDecimal price;

    @Enumerated(EnumType.STRING)
    @Column
    private EventStatus status;

    @Column(name = "created_at")
    private LocalDateTime createdAt;


}
