package com.kashif.event_registration_platform.event.service;

import com.kashif.event_registration_platform.auth.entity.User;
import com.kashif.event_registration_platform.event.dto.EventRequest;
import com.kashif.event_registration_platform.event.dto.EventResponse;

import java.util.List;

public interface EventService {
    EventResponse createEvent(EventRequest request, User organiser);
    EventResponse publishEvent(Long eventId, User organiser);
    EventResponse unpublishEvent(Long eventId, User organiser);
    EventResponse cancelEvent(Long eventId, User organiser);
    List<EventResponse> getPublishedEvents();

}
