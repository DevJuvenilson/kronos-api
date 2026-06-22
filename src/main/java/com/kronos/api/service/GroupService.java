package com.kronos.api.service;

import com.kronos.api.dto.request.GroupRequestDTO;
import com.kronos.api.dto.response.GroupMemberResponseDTO;
import com.kronos.api.dto.response.GroupResponseDTO;
import com.kronos.api.dto.response.UserResponseDTO;
import com.kronos.api.model.Group;
import com.kronos.api.model.GroupMember;
import com.kronos.api.model.User;
import com.kronos.api.model.enums.GroupRole;
import com.kronos.api.repository.GroupMemberRepository;
import com.kronos.api.repository.GroupRepository;
import com.kronos.api.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GroupService {

    private final GroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final UserRepository userRepository;

    @Transactional
    public GroupResponseDTO create(String authenticatedEmail, GroupRequestDTO request) {
        User authenticatedUser = findAuthenticatedUser(authenticatedEmail);

        Group group = new Group();
        group.setName(request.name().trim());

        Group savedGroup = groupRepository.save(group);
        GroupMember ownerMembership = new GroupMember();
        ownerMembership.setUser(authenticatedUser);
        ownerMembership.setGroup(savedGroup);
        ownerMembership.setGroupRole(GroupRole.OWNER);
        groupMemberRepository.save(ownerMembership);

        return toGroupResponse(savedGroup, true);
    }

    @Transactional(readOnly = true)
    public List<GroupMemberResponseDTO> listAuthenticatedUserGroups(String authenticatedEmail) {
        User authenticatedUser = findAuthenticatedUser(authenticatedEmail);

        return groupMemberRepository.findByUserId(authenticatedUser.getId()).stream()
                .sorted(Comparator.comparing(member -> member.getGroup().getName(), String.CASE_INSENSITIVE_ORDER))
                .map(member -> toGroupMemberResponse(member, canViewInvitationCode(member)))
                .toList();
    }

    @Transactional(readOnly = true)
    public GroupResponseDTO findByUuid(String authenticatedEmail, UUID groupUuid) {
        User authenticatedUser = findAuthenticatedUser(authenticatedEmail);
        Group group = findGroupByUuid(groupUuid);
        GroupMember membership = findMembership(authenticatedUser, group);

        return toGroupResponse(group, canViewInvitationCode(membership));
    }

    private User findAuthenticatedUser(String email) {
        return userRepository.findByEmailIgnoreCase(normalizeEmail(email))
                .orElseThrow(() -> new EntityNotFoundException("Authenticated user not found"));
    }

    private Group findGroupByUuid(UUID groupUuid) {
        return groupRepository.findByUuid(groupUuid)
                .orElseThrow(() -> new EntityNotFoundException("Group not found"));
    }

    private GroupMember findMembership(User user, Group group) {
        return groupMemberRepository.findByUserIdAndGroupId(user.getId(), group.getId())
                .orElseThrow(() -> new AccessDeniedException("User does not belong to this group"));
    }

    private GroupMemberResponseDTO toGroupMemberResponse(GroupMember membership, boolean includeInvitationCode) {
        return new GroupMemberResponseDTO(
                toUserResponse(membership.getUser()),
                toGroupResponse(membership.getGroup(), includeInvitationCode),
                membership.getGroupRole()
        );
    }

    private GroupResponseDTO toGroupResponse(Group group, boolean includeInvitationCode) {
        return new GroupResponseDTO(
                group.getUuid(),
                group.getName(),
                includeInvitationCode ? group.getInvitationCode() : null
        );
    }

    private UserResponseDTO toUserResponse(User user) {
        return new UserResponseDTO(user.getUuid(), user.getName(), user.getEmail(), user.getUsername());
    }

    private boolean canViewInvitationCode(GroupMember membership) {
        return membership.getGroupRole() == GroupRole.OWNER || membership.getGroupRole() == GroupRole.ADMIN;
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
