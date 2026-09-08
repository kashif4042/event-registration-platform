package com.kashif.event_registration_platform.event.service;

import com.kashif.event_registration_platform.auth.entity.User;
import com.kashif.event_registration_platform.common.exception.InvalidStateTransitionException;
import com.kashif.event_registration_platform.common.exception.ResourceNotFoundException;
import com.kashif.event_registration_platform.common.exception.UnauthorizedActionException;
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
    public EventResponse createEvent(EventRequest request, User organiser) {
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

    @Override
    public EventResponse publishEvent(Long eventId, User organiser) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found"));

        if (!event.getOrganiser().getId().equals(organiser.getId())) {
            throw new UnauthorizedActionException("You are not authorized to modify this event");
        }

        if (!event.getStatus().equals(EventStatus.DRAFT)) {
            throw new InvalidStateTransitionException("Only a DRAFT event can be published");
        }

        event.setStatus(EventStatus.PUBLISHED);
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
    @Override
    public EventResponse unpublishEvent(Long eventId, User organiser){
        Event event = eventRepository.findById(eventId)
                .orElseThrow(()->new ResourceNotFoundException("Event Not Found"));
        if (!event.getOrganiser().getId().equals(organiser.getId())) {
            throw new UnauthorizedActionException("You are not authorized to modify this event");
        }

        if (!event.getStatus().equals(EventStatus.PUBLISHED)) {
            throw new InvalidStateTransitionException("Only a PUBLISHED event can be unpublished");
        }

        event.setStatus(EventStatus.DRAFT);
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
    @Override
    public EventResponse cancelEvent(Long eventId, User organiser) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event Not Found"));

        if (!event.getOrganiser().getId().equals(organiser.getId())) {
            throw new UnauthorizedActionException("You are not authorized to modify this event");
        }

        if (event.getStatus() != EventStatus.DRAFT && event.getStatus() != EventStatus.PUBLISHED) {
            throw new InvalidStateTransitionException("Only a DRAFT or PUBLISHED event can be cancelled");
        }

        event.setStatus(EventStatus.CANCELLED);
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
