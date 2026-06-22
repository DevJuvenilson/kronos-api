package com.kronos.api.service;

import com.kronos.api.dto.request.ForgotPasswordRequestDTO;
import com.kronos.api.dto.request.UserLoginRequestDTO;
import com.kronos.api.dto.request.UserRegisterRequestDTO;
import com.kronos.api.dto.response.LoginResponseDTO;
import com.kronos.api.dto.response.UserResponseDTO;
import com.kronos.api.infra.security.TokenService;
import com.kronos.api.model.User;
import com.kronos.api.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final TokenService tokenService;

    @Transactional
    public UserResponseDTO register(UserRegisterRequestDTO request) {
        String email = normalizeEmail(request.email());
        String username = normalizeUsername(request.username());

        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new DataIntegrityViolationException("Email already in use");
        }

        if (userRepository.existsByUsernameIgnoreCase(username)) {
            throw new DataIntegrityViolationException("Username already in use");
        }

        User user = new User();
        user.setName(request.name().trim());
        user.setEmail(email);
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(request.password()));

        return toUserResponse(userRepository.save(user));
    }

    @Transactional(readOnly = true)
    public LoginResponseDTO login(UserLoginRequestDTO request) {
        String loginIdentifier = resolveLoginIdentifier(request);

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginIdentifier, request.password())
        );

        User user = findByLoginIdentifier(loginIdentifier)
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        String token = tokenService.generateToken(user);

        return new LoginResponseDTO(token, "Bearer", toUserResponse(user));
    }

    @Transactional
    public void resetForgottenPassword(ForgotPasswordRequestDTO request) {
        User user = userRepository.findByEmailIgnoreCase(normalizeEmail(request.email()))
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        user.setPassword(passwordEncoder.encode(request.newPassword()));
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

    private String resolveLoginIdentifier(UserLoginRequestDTO request) {
        if (hasText(request.email())) {
            return normalizeEmail(request.email());
        }

        return normalizeUsername(request.username());
    }

    private java.util.Optional<User> findByLoginIdentifier(String loginIdentifier) {
        if (loginIdentifier.contains("@")) {
            return userRepository.findByEmailIgnoreCase(loginIdentifier);
        }

        return userRepository.findByUsernameIgnoreCase(loginIdentifier);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
