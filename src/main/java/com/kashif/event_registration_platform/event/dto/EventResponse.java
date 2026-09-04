package com.kashif.event_registration_platform.event.dto;

import com.kashif.event_registration_platform.event.entity.EventStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EventResponse {
        private Long id;
        private String title;
        private String venue;
        private LocalDateTime eventDate;
        private Integer maxCapacity;
        private BigDecimal price;
        private EventStatus status;
        private LocalDateTime createdAt;
        private String organiserName;
}
