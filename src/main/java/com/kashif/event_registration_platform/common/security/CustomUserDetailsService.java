package com.kashif.event_registration_platform.common.security;


import com.kashif.event_registration_platform.auth.entity.User;
import com.kashif.event_registration_platform.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException{
        User user = userRepository.findByEmail(email)
                .orElseThrow(()-> new UsernameNotFoundException("User not found with username: " + email));
        return new CustomUserDetails(user);

    }

}
