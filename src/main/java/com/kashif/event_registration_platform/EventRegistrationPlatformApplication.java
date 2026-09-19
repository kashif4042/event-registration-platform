package com.kashif.event_registration_platform;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableAsync
@EnableScheduling
public class EventRegistrationPlatformApplication {

	public static void main(String[] args) {
		SpringApplication.run(EventRegistrationPlatformApplication.class, args);
	}

}
