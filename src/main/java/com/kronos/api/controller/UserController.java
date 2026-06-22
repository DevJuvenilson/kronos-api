package com.kronos.api.controller;

import com.kronos.api.dto.request.ChangePasswordRequestDTO;
import com.kronos.api.dto.request.UserUpdateRequestDTO;
import com.kronos.api.dto.response.UserResponseDTO;
import com.kronos.api.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users/me")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PatchMapping
    public ResponseEntity<UserResponseDTO> updateProfile(
            @AuthenticationPrincipal UserDetails authenticatedUser,
            @RequestBody @Valid UserUpdateRequestDTO request
    ) {
        UserResponseDTO response = userService.updateProfile(authenticatedUser.getUsername(), request);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/password")
    public ResponseEntity<Void> changePassword(
            @AuthenticationPrincipal UserDetails authenticatedUser,
            @RequestBody @Valid ChangePasswordRequestDTO request
    ) {
        userService.changePassword(authenticatedUser.getUsername(), request);
        return ResponseEntity.noContent().build();
    }
}
