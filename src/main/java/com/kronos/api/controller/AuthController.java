package com.kronos.api.controller;

import com.kronos.api.dto.request.ForgotPasswordRequestDTO;
import com.kronos.api.dto.request.UserLoginRequestDTO;
import com.kronos.api.dto.request.UserRegisterRequestDTO;
import com.kronos.api.dto.response.LoginResponseDTO;
import com.kronos.api.dto.response.UserResponseDTO;
import com.kronos.api.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<UserResponseDTO> register(@RequestBody @Valid UserRegisterRequestDTO request) {
        UserResponseDTO response = authService.register(request);
        return ResponseEntity.status(HttpStatusCode.valueOf(201)).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(@RequestBody @Valid UserLoginRequestDTO request) {
        LoginResponseDTO response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/forgot-password")
    public ResponseEntity<Void> resetForgottenPassword(@RequestBody @Valid ForgotPasswordRequestDTO request) {
        authService.resetForgottenPassword(request);
        return ResponseEntity.noContent().build();
    }
}
