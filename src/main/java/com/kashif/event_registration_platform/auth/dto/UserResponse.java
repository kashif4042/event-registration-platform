package com.kashif.event_registration_platform.auth.dto;

import com.kashif.event_registration_platform.auth.entity.Role;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UserResponse {
    private Long id;
    private String name;
    private String email;
    private Role role;
    private Boolean isVerified;
}
