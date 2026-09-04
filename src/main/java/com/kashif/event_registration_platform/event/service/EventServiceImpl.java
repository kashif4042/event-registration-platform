package com.kashif.event_registration_platform.event.service;

import com.kashif.event_registration_platform.auth.entity.User;
import com.kashif.event_registration_platform.event.dto.EventRequest;
import com.kashif.event_registration_platform.event.dto.EventResponse;
import com.kashif.event_registration_platform.event.entity.Event;
import com.kashif.event_registration_platform.event.entity.EventStatus;
import com.kashif.event_registration_platform.event.repository.EventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class EventServiceImpl implements EventService {
    private final EventRepository eventRepository;

    @Override
    public EventResponse createEvent(EventRequest request, User organiser){
        Event event = new Event();
        event.setTitle(request.getTitle());
        event.setVenue(request.getVenue());
        event.setEventDate(request.getEventDate());
        event.setMaxCapacity(request.getMaxCapacity());
        event.setPrice(request.getPrice());
        event.setOrganiser(organiser);
        event.setStatus(EventStatus.DRAFT);
        event.setCreatedAt(LocalDateTime.now());
        Event savedEvent = eventRepository.save(event);
        return new EventResponse(savedEvent.getId(),
                savedEvent.getTitle(),
                savedEvent.getVenue(),
                savedEvent.getEventDate(),
                savedEvent.getMaxCapacity(),
                savedEvent.getPrice(),
                savedEvent.getStatus(),
                savedEvent.getCreatedAt(),
                savedEvent.getOrganiser().getName());
    }
}
