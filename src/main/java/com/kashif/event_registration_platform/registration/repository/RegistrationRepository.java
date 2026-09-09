package com.kashif.event_registration_platform.registration.repository;

import com.kashif.event_registration_platform.auth.entity.User;
import com.kashif.event_registration_platform.event.entity.Event;
import com.kashif.event_registration_platform.registration.entity.Registration;
import com.kashif.event_registration_platform.registration.entity.RegistrationStatus;


import org.springframework.data.jpa.repository.JpaRepository;


import java.util.List;
import java.util.Optional;


public interface RegistrationRepository extends JpaRepository<Registration,Long> {
     long countByEventAndStatus(Event event, RegistrationStatus status);
     boolean existsByUserAndEventAndStatusIn(User user, Event event, List<RegistrationStatus> statuses);
     Optional<Registration> findFirstByEventAndStatusOrderByRegisteredAtAsc(Event event, RegistrationStatus status);

}
