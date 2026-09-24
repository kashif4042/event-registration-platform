package com.kashif.event_registration_platform.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class RevenueSummaryResponse {
    private List<EventRevenue> eventBreakdown;
    private BigDecimal totalRevenue;
}
