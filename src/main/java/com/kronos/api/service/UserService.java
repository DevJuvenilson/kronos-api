package com.kronos.api.service;

import com.kronos.api.dto.request.ChangePasswordRequestDTO;
import com.kronos.api.dto.request.UserUpdateRequestDTO;
import com.kronos.api.dto.response.UserResponseDTO;
import com.kronos.api.model.User;
import com.kronos.api.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public UserResponseDTO updateProfile(String authenticatedEmail, UserUpdateRequestDTO request) {
        User user = findAuthenticatedUser(authenticatedEmail);

        if (request.name() != null) {
            user.setName(request.name().trim());
        }

        if (request.username() != null) {
            String username = normalizeUsername(request.username());
            if (!username.equalsIgnoreCase(user.getUsername()) && userRepository.existsByUsernameIgnoreCase(username)) {
                throw new DataIntegrityViolationException("Username already in use");
            }

            user.setUsername(username);
        }

        return toUserResponse(user);
    }

    @Transactional
    public void changePassword(String authenticatedEmail, ChangePasswordRequestDTO request) {
        User user = findAuthenticatedUser(authenticatedEmail);

        if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
            throw new BadCredentialsException("Invalid email or password");
        }

        user.setPassword(passwordEncoder.encode(request.newPassword()));
    }

    private User findAuthenticatedUser(String email) {
        return userRepository.findByEmailIgnoreCase(normalizeEmail(email))
                .orElseThrow(() -> new EntityNotFoundException("Authenticated user not found"));
    }

    private UserResponseDTO toUserResponse(User user) {
        return new UserResponseDTO(user.getUuid(), user.getName(), user.getEmail(), user.getUsername());
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeUsername(String username) {
        return username.trim().toLowerCase(Locale.ROOT);
    }
}
