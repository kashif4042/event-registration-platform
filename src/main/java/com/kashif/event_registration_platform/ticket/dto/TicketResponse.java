package com.kashif.event_registration_platform.ticket.dto;

import com.kashif.event_registration_platform.ticket.entity.TicketStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TicketResponse {
    private Long id;
    private UUID token;
    private TicketStatus status;
    private LocalDateTime issuedAt;
}
