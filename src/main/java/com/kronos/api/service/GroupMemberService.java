package com.kronos.api.service;

import com.kronos.api.dto.request.GroupMemberAddRequestDTO;
import com.kronos.api.dto.request.GroupMemberRoleUpdateRequestDTO;
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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GroupMemberService {

    private final GroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final UserRepository userRepository;

    @Transactional
    public GroupMemberResponseDTO joinByInvitationCode(String authenticatedEmail, String invitationCode) {
        User authenticatedUser = findAuthenticatedUser(authenticatedEmail);
        Group group = groupRepository.findByInvitationCode(normalizeInvitationCode(invitationCode))
                .orElseThrow(() -> new EntityNotFoundException("Group not found"));

        ensureUserIsNotAlreadyMember(authenticatedUser, group);

        GroupMember membership = new GroupMember();
        membership.setUser(authenticatedUser);
        membership.setGroup(group);
        membership.setGroupRole(GroupRole.MEMBER);

        return toGroupMemberResponse(groupMemberRepository.save(membership), false);
    }

    @Transactional(readOnly = true)
    public List<GroupMemberResponseDTO> listMembers(String authenticatedEmail, UUID groupUuid) {
        User authenticatedUser = findAuthenticatedUser(authenticatedEmail);
        Group group = findGroupByUuid(groupUuid);
        GroupMember requesterMembership = findMembership(authenticatedUser, group);
        boolean includeInvitationCode = canViewInvitationCode(requesterMembership);

        return groupMemberRepository.findByGroupId(group.getId()).stream()
                .sorted(Comparator
                        .comparingInt((GroupMember member) -> rolePriority(member.getGroupRole()))
                        .thenComparing(member -> member.getUser().getName(), String.CASE_INSENSITIVE_ORDER))
                .map(member -> toGroupMemberResponse(member, includeInvitationCode))
                .toList();
    }

    @Transactional
    public GroupMemberResponseDTO addMember(String authenticatedEmail, UUID groupUuid, GroupMemberAddRequestDTO request) {
        User authenticatedUser = findAuthenticatedUser(authenticatedEmail);
        Group group = findGroupByUuid(groupUuid);
        GroupMember requesterMembership = findMembership(authenticatedUser, group);
        ensureCanManageMembers(requesterMembership);

        User targetUser = findUserByIdentifier(request.userIdentifier());
        ensureUserIsNotAlreadyMember(targetUser, group);

        GroupMember membership = new GroupMember();
        membership.setUser(targetUser);
        membership.setGroup(group);
        membership.setGroupRole(GroupRole.MEMBER);

        return toGroupMemberResponse(
                groupMemberRepository.save(membership),
                canViewInvitationCode(requesterMembership)
        );
    }

    @Transactional
    public GroupMemberResponseDTO updateRole(
            String authenticatedEmail,
            UUID groupUuid,
            UUID userUuid,
            GroupMemberRoleUpdateRequestDTO request
    ) {
        User authenticatedUser = findAuthenticatedUser(authenticatedEmail);
        Group group = findGroupByUuid(groupUuid);
        GroupMember requesterMembership = findMembership(authenticatedUser, group);
        ensureOwner(requesterMembership);

        GroupMember targetMembership = findMembershipByUserUuid(group, userUuid);
        ensureTargetIsNotOwner(targetMembership);
        ensureRoleCanBeAssigned(request.groupRole());

        targetMembership.setGroupRole(request.groupRole());

        return toGroupMemberResponse(targetMembership, true);
    }

    @Transactional
    public void removeMember(String authenticatedEmail, UUID groupUuid, UUID userUuid) {
        User authenticatedUser = findAuthenticatedUser(authenticatedEmail);
        Group group = findGroupByUuid(groupUuid);
        GroupMember requesterMembership = findMembership(authenticatedUser, group);
        ensureCanManageMembers(requesterMembership);

        GroupMember targetMembership = findMembershipByUserUuid(group, userUuid);
        ensureCanRemoveMember(requesterMembership, targetMembership);

        groupMemberRepository.delete(targetMembership);
    }

    private User findAuthenticatedUser(String email) {
        return userRepository.findByEmailIgnoreCase(normalizeEmail(email))
                .orElseThrow(() -> new EntityNotFoundException("Authenticated user not found"));
    }

    private User findUserByIdentifier(String userIdentifier) {
        String normalizedIdentifier = userIdentifier.trim().toLowerCase(Locale.ROOT);
        Optional<User> user = normalizedIdentifier.contains("@")
                ? userRepository.findByEmailIgnoreCase(normalizedIdentifier)
                : userRepository.findByUsernameIgnoreCase(normalizedIdentifier);

        return user.orElseThrow(() -> new EntityNotFoundException("User not found"));
    }

    private Group findGroupByUuid(UUID groupUuid) {
        return groupRepository.findByUuid(groupUuid)
                .orElseThrow(() -> new EntityNotFoundException("Group not found"));
    }

    private GroupMember findMembership(User user, Group group) {
        return groupMemberRepository.findByUserIdAndGroupId(user.getId(), group.getId())
                .orElseThrow(() -> new AccessDeniedException("User does not belong to this group"));
    }

    private GroupMember findMembershipByUserUuid(Group group, UUID userUuid) {
        return groupMemberRepository.findByUserUuidAndGroupId(userUuid, group.getId())
                .orElseThrow(() -> new EntityNotFoundException("Group member not found"));
    }

    private void ensureUserIsNotAlreadyMember(User user, Group group) {
        groupMemberRepository.findByUserIdAndGroupId(user.getId(), group.getId())
                .ifPresent(member -> {
                    throw new DataIntegrityViolationException("User already belongs to this group");
                });
    }

    private void ensureCanManageMembers(GroupMember requesterMembership) {
        if (requesterMembership.getGroupRole() == GroupRole.MEMBER) {
            throw new AccessDeniedException("Only group owners and admins can manage members");
        }
    }

    private void ensureOwner(GroupMember requesterMembership) {
        if (requesterMembership.getGroupRole() != GroupRole.OWNER) {
            throw new AccessDeniedException("Only group owners can manage admin roles");
        }
    }

    private void ensureTargetIsNotOwner(GroupMember targetMembership) {
        if (targetMembership.getGroupRole() == GroupRole.OWNER) {
            throw new AccessDeniedException("Owner role cannot be changed by this endpoint");
        }
    }

    private void ensureRoleCanBeAssigned(GroupRole role) {
        if (role == GroupRole.OWNER) {
            throw new AccessDeniedException("Owner role cannot be assigned by this endpoint");
        }
    }

    private void ensureCanRemoveMember(GroupMember requesterMembership, GroupMember targetMembership) {
        if (targetMembership.getGroupRole() == GroupRole.OWNER) {
            throw new AccessDeniedException("Group owners cannot be removed from the group");
        }

        if (requesterMembership.getGroupRole() == GroupRole.ADMIN
                && targetMembership.getGroupRole() == GroupRole.ADMIN) {
            throw new AccessDeniedException("Admins cannot remove other admins");
        }
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

    private int rolePriority(GroupRole role) {
        return switch (role) {
            case OWNER -> 0;
            case ADMIN -> 1;
            case MEMBER -> 2;
        };
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeInvitationCode(String invitationCode) {
        return invitationCode.trim().toUpperCase(Locale.ROOT);
    }
}
