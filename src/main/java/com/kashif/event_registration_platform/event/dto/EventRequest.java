package com.kashif.event_registration_platform.event.dto;


import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class EventRequest {
    @NotBlank(message = "Title is required")
    private String title;

    @NotBlank(message = "Venue is required")
    private String venue;

    @Future
    @NotNull
    private LocalDateTime eventDate;

    @NotNull
    @Positive
    private Integer maxCapacity;

    @NotNull
    @Positive
    private BigDecimal price;


}
