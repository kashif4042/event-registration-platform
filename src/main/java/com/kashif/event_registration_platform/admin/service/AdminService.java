package com.kashif.event_registration_platform.admin.service;

import com.kashif.event_registration_platform.admin.dto.RevenueSummaryResponse;
import com.kashif.event_registration_platform.admin.dto.StatsResponse;
import com.kashif.event_registration_platform.auth.entity.User;

public interface AdminService {
    StatsResponse getEventStats(Long eventId , User organiser);
    RevenueSummaryResponse getRevenueSummary(User organiser);

}
