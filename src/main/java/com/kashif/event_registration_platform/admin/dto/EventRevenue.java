package com.kashif.event_registration_platform.admin.dto;

import lombok.*;
import java.math.BigDecimal;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class EventRevenue {
    private Long eventId;
    private String eventTitle;
    private BigDecimal revenue;
}