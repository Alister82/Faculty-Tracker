package com.college.tracker.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Optional;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private AppUserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String usernameOrEmail) throws UsernameNotFoundException {
        // First try to find by username, then fall back to email
        Optional<AppUser> userOpt = userRepository.findByUsername(usernameOrEmail);
        if (userOpt.isEmpty()) {
            userOpt = userRepository.findByEmail(usernameOrEmail);
        }

        AppUser user = userOpt.orElseThrow(() ->
                new UsernameNotFoundException("No account found with username or email: " + usernameOrEmail));

        return new AppUserDetails(
                user.getUsername(),
                user.getPassword(),
                Collections.singletonList(new SimpleGrantedAuthority(user.getRole().name())),
                user.getDepartment()
        );
    }
}
