package com.kashif.event_registration_platform.admin.dto;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class StatsResponse {

    private Long eventId;
    private String eventTitle;
    private Long totalConfirmed;
    private Long totalWaitlisted;
    private Long totalCheckedIn;
    private Double checkInRate;
}
