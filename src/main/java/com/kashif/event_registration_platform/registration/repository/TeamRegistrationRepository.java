package com.kashif.event_registration_platform.registration.repository;

import com.kashif.event_registration_platform.registration.entity.TeamRegistration;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TeamRegistrationRepository extends JpaRepository<TeamRegistration,Long> {

}
