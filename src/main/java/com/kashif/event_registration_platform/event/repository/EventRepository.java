package com.kashif.event_registration_platform.event.repository;

import com.kashif.event_registration_platform.auth.entity.User;
import com.kashif.event_registration_platform.event.entity.Event;
import com.kashif.event_registration_platform.event.entity.EventStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EventRepository extends JpaRepository<Event,Long> {
    List<Event> findByOrganiser(User organiser);
    List<Event> findByStatus(EventStatus status);

}
