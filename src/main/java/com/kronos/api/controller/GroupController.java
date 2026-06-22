package com.kronos.api.controller;

import com.kronos.api.dto.request.GroupRequestDTO;
import com.kronos.api.dto.response.GroupMemberResponseDTO;
import com.kronos.api.dto.response.GroupResponseDTO;
import com.kronos.api.service.GroupService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/groups")
@RequiredArgsConstructor
public class GroupController {

    private final GroupService groupService;

    @PostMapping
    public ResponseEntity<GroupResponseDTO> create(
            @AuthenticationPrincipal UserDetails authenticatedUser,
            @RequestBody @Valid GroupRequestDTO request
    ) {
        GroupResponseDTO response = groupService.create(authenticatedUser.getUsername(), request);
        return ResponseEntity.status(HttpStatusCode.valueOf(201)).body(response);
    }

    @GetMapping
    public ResponseEntity<List<GroupMemberResponseDTO>> listAuthenticatedUserGroups(
            @AuthenticationPrincipal UserDetails authenticatedUser
    ) {
        List<GroupMemberResponseDTO> response = groupService.listAuthenticatedUserGroups(authenticatedUser.getUsername());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{groupUuid}")
    public ResponseEntity<GroupResponseDTO> findByUuid(
            @AuthenticationPrincipal UserDetails authenticatedUser,
            @PathVariable UUID groupUuid
    ) {
        GroupResponseDTO response = groupService.findByUuid(authenticatedUser.getUsername(), groupUuid);
        return ResponseEntity.ok(response);
    }
}
