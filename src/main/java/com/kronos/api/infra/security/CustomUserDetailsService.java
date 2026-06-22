package com.kronos.api.infra.security;

import com.kronos.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Locale;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String loginIdentifier) throws UsernameNotFoundException {
        String normalizedLoginIdentifier = loginIdentifier.trim().toLowerCase(Locale.ROOT);

        com.kronos.api.model.User user = findByLoginIdentifier(normalizedLoginIdentifier)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        return org.springframework.security.core.userdetails.User
                .withUsername(user.getEmail())
                .password(user.getPassword())
                .authorities(Collections.emptyList())
                .build();
    }

    private Optional<com.kronos.api.model.User> findByLoginIdentifier(String loginIdentifier) {
        if (loginIdentifier.contains("@")) {
            return userRepository.findByEmailIgnoreCase(loginIdentifier);
        }

        return userRepository.findByUsernameIgnoreCase(loginIdentifier);
    }
}
