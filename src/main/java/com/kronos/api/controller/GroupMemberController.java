package com.kronos.api.controller;

import com.kronos.api.dto.request.GroupMemberAddRequestDTO;
import com.kronos.api.dto.request.GroupMemberRoleUpdateRequestDTO;
import com.kronos.api.dto.response.GroupMemberResponseDTO;
import com.kronos.api.service.GroupMemberService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
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
public class GroupMemberController {

    private final GroupMemberService groupMemberService;

    @PostMapping("/join/{invitationCode}")
    public ResponseEntity<GroupMemberResponseDTO> joinByInvitationCode(
            @AuthenticationPrincipal UserDetails authenticatedUser,
            @PathVariable String invitationCode
    ) {
        GroupMemberResponseDTO response = groupMemberService.joinByInvitationCode(
                authenticatedUser.getUsername(),
                invitationCode
        );
        return ResponseEntity.status(HttpStatusCode.valueOf(201)).body(response);
    }

    @GetMapping("/{groupUuid}/members")
    public ResponseEntity<List<GroupMemberResponseDTO>> listMembers(
            @AuthenticationPrincipal UserDetails authenticatedUser,
            @PathVariable UUID groupUuid
    ) {
        List<GroupMemberResponseDTO> response = groupMemberService.listMembers(authenticatedUser.getUsername(), groupUuid);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{groupUuid}/members")
    public ResponseEntity<GroupMemberResponseDTO> addMember(
            @AuthenticationPrincipal UserDetails authenticatedUser,
            @PathVariable UUID groupUuid,
            @RequestBody @Valid GroupMemberAddRequestDTO request
    ) {
        GroupMemberResponseDTO response = groupMemberService.addMember(
                authenticatedUser.getUsername(),
                groupUuid,
                request
        );
        return ResponseEntity.status(HttpStatusCode.valueOf(201)).body(response);
    }

    @PatchMapping("/{groupUuid}/members/{userUuid}/role")
    public ResponseEntity<GroupMemberResponseDTO> updateRole(
            @AuthenticationPrincipal UserDetails authenticatedUser,
            @PathVariable UUID groupUuid,
            @PathVariable UUID userUuid,
            @RequestBody @Valid GroupMemberRoleUpdateRequestDTO request
    ) {
        GroupMemberResponseDTO response = groupMemberService.updateRole(
                authenticatedUser.getUsername(),
                groupUuid,
                userUuid,
                request
        );
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{groupUuid}/members/{userUuid}")
    public ResponseEntity<Void> removeMember(
            @AuthenticationPrincipal UserDetails authenticatedUser,
            @PathVariable UUID groupUuid,
            @PathVariable UUID userUuid
    ) {
        groupMemberService.removeMember(authenticatedUser.getUsername(), groupUuid, userUuid);
        return ResponseEntity.noContent().build();
    }
}
