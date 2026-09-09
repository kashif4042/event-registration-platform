package com.kashif.event_registration_platform.event.repository;

import com.kashif.event_registration_platform.auth.entity.User;
import com.kashif.event_registration_platform.event.entity.Event;
import com.kashif.event_registration_platform.event.entity.EventStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface EventRepository extends JpaRepository<Event,Long> {
    List<Event> findByOrganiser(User organiser);
    List<Event> findByStatus(EventStatus status);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT e FROM Event e WHERE e.id = :id")
    Optional<Event> findByIdForUpdate(@Param("id") Long id);

}
