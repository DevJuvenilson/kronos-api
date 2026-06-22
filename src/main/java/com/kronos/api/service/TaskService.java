package com.kronos.api.service;

import com.kronos.api.dto.request.TaskRequestDTO;
import com.kronos.api.dto.request.TaskUpdateRequestDTO;
import com.kronos.api.dto.response.GroupResponseDTO;
import com.kronos.api.dto.response.TaskResponseDTO;
import com.kronos.api.dto.response.UserResponseDTO;
import com.kronos.api.model.Group;
import com.kronos.api.model.GroupMember;
import com.kronos.api.model.Task;
import com.kronos.api.model.User;
import com.kronos.api.model.enums.GroupRole;
import com.kronos.api.repository.GroupMemberRepository;
import com.kronos.api.repository.GroupRepository;
import com.kronos.api.repository.TaskRepository;
import com.kronos.api.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;
    private final GroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final UserRepository userRepository;

    @Transactional
    public TaskResponseDTO create(String authenticatedEmail, TaskRequestDTO request) {
        User authenticatedUser = findAuthenticatedUser(authenticatedEmail);
        Group group = findGroupByUuid(request.groupUuid());
        ensureCanManageTasks(authenticatedUser, group);

        Task task = new Task();
        task.setTitle(request.title().trim());
        task.setDescription(request.description().trim());
        task.setDeadline(request.deadline());
        task.setCategory(normalizeCategory(request.category()));
        task.setGroup(group);
        task.setCreatedBy(authenticatedUser);

        return toTaskResponse(taskRepository.save(task), true);
    }

    @Transactional(readOnly = true)
    public List<TaskResponseDTO> listByGroup(String authenticatedEmail, UUID groupUuid) {
        User authenticatedUser = findAuthenticatedUser(authenticatedEmail);
        Group group = findGroupByUuid(groupUuid);
        GroupMember membership = findMembership(authenticatedUser, group);
        boolean includeInvitationCode = canViewInvitationCode(membership);

        return taskRepository.findByGroupIdOrderByDeadlineAsc(group.getId()).stream()
                .map(task -> toTaskResponse(task, includeInvitationCode))
                .toList();
    }

    @Transactional
    public TaskResponseDTO update(String authenticatedEmail, UUID taskUuid, TaskUpdateRequestDTO request) {
        User authenticatedUser = findAuthenticatedUser(authenticatedEmail);
        Task task = findTaskByUuid(taskUuid);
        ensureCanManageTasks(authenticatedUser, task.getGroup());

        task.setTitle(request.title().trim());
        task.setDescription(request.description().trim());
        task.setDeadline(request.deadline());
        task.setCategory(normalizeCategory(request.category()));

        return toTaskResponse(task, true);
    }

    @Transactional
    public void delete(String authenticatedEmail, UUID taskUuid) {
        User authenticatedUser = findAuthenticatedUser(authenticatedEmail);
        Task task = findTaskByUuid(taskUuid);
        ensureCanManageTasks(authenticatedUser, task.getGroup());

        taskRepository.delete(task);
    }

    private User findAuthenticatedUser(String email) {
        return userRepository.findByEmailIgnoreCase(normalizeEmail(email))
                .orElseThrow(() -> new EntityNotFoundException("Authenticated user not found"));
    }

    private Group findGroupByUuid(UUID groupUuid) {
        return groupRepository.findByUuid(groupUuid)
                .orElseThrow(() -> new EntityNotFoundException("Group not found"));
    }

    private Task findTaskByUuid(UUID taskUuid) {
        return taskRepository.findByUuid(taskUuid)
                .orElseThrow(() -> new EntityNotFoundException("Task not found"));
    }

    private GroupMember findMembership(User user, Group group) {
        return groupMemberRepository.findByUserIdAndGroupId(user.getId(), group.getId())
                .orElseThrow(() -> new AccessDeniedException("User does not belong to this group"));
    }

    private void ensureCanManageTasks(User user, Group group) {
        GroupMember membership = findMembership(user, group);

        if (membership.getGroupRole() == GroupRole.MEMBER) {
            throw new AccessDeniedException("Only group owners and admins can manage tasks");
        }
    }

    private TaskResponseDTO toTaskResponse(Task task, boolean includeInvitationCode) {
        return new TaskResponseDTO(
                task.getUuid(),
                task.getTitle(),
                task.getDescription(),
                task.getDeadline(),
                task.getCategory(),
                toGroupResponse(task.getGroup(), includeInvitationCode),
                toUserResponse(task.getCreatedBy())
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

    private String normalizeCategory(String category) {
        if (category == null || category.isBlank()) {
            return null;
        }

        return category.trim();
    }
}
