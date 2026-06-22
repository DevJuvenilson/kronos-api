package com.kronos.api.service;

import com.kronos.api.dto.request.TaskRequestDTO;
import com.kronos.api.dto.request.TaskUpdateRequestDTO;
import com.kronos.api.dto.response.TaskResponseDTO;
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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    private static final Long USER_ID = 10L;
    private static final Long GROUP_ID = 20L;
    private static final Long TASK_ID = 30L;

    private static final UUID USER_UUID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID GROUP_UUID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID TASK_UUID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    private static final String AUTHENTICATED_EMAIL = "owner@example.com";
    private static final LocalDateTime DEADLINE = LocalDateTime.of(2026, 7, 1, 9, 30);

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private GroupRepository groupRepository;

    @Mock
    private GroupMemberRepository groupMemberRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private TaskService taskService;

    @Nested
    @DisplayName("Create task")
    class CreateTask {

        @Test
        @DisplayName("given owner and valid request, when creating task, then persists normalized task and returns response")
        void givenOwnerAndValidRequest_whenCreateTask_thenPersistNormalizedTaskAndReturnResponse() {
            // Arrange
            User owner = user();
            Group group = group();
            GroupMember ownerMembership = membership(owner, group, GroupRole.OWNER);
            TaskRequestDTO request = new TaskRequestDTO(
                    "  Prepare backend tests  ",
                    "  Cover service behavior  ",
                    DEADLINE,
                    "  Quality  ",
                    GROUP_UUID
            );

            given(userRepository.findByEmailIgnoreCase(AUTHENTICATED_EMAIL)).willReturn(Optional.of(owner));
            given(groupRepository.findByUuid(GROUP_UUID)).willReturn(Optional.of(group));
            given(groupMemberRepository.findByUserIdAndGroupId(USER_ID, GROUP_ID)).willReturn(Optional.of(ownerMembership));
            given(taskRepository.save(any(Task.class))).willAnswer(invocation -> {
                Task savedTask = invocation.getArgument(0);
                savedTask.setId(TASK_ID);
                savedTask.setUuid(TASK_UUID);
                return savedTask;
            });

            // Act
            TaskResponseDTO response = taskService.create("  OWNER@EXAMPLE.COM  ", request);

            // Assert
            ArgumentCaptor<Task> taskCaptor = ArgumentCaptor.forClass(Task.class);
            verify(taskRepository, times(1)).save(taskCaptor.capture());

            Task persistedTask = taskCaptor.getValue();
            assertThat(persistedTask.getTitle()).isEqualTo("Prepare backend tests");
            assertThat(persistedTask.getDescription()).isEqualTo("Cover service behavior");
            assertThat(persistedTask.getDeadline()).isEqualTo(DEADLINE);
            assertThat(persistedTask.getCategory()).isEqualTo("Quality");
            assertThat(persistedTask.getGroup()).isSameAs(group);
            assertThat(persistedTask.getCreatedBy()).isSameAs(owner);

            assertThat(response.uuid()).isEqualTo(TASK_UUID);
            assertThat(response.title()).isEqualTo("Prepare backend tests");
            assertThat(response.description()).isEqualTo("Cover service behavior");
            assertThat(response.deadline()).isEqualTo(DEADLINE);
            assertThat(response.category()).isEqualTo("Quality");
            assertThat(response.group().uuid()).isEqualTo(GROUP_UUID);
            assertThat(response.group().invitationCode()).isEqualTo("INV123");
            assertThat(response.createdBy().uuid()).isEqualTo(USER_UUID);
            assertThat(response.createdBy().email()).isEqualTo(AUTHENTICATED_EMAIL);
        }

        @Test
        @DisplayName("given blank category, when creating task, then persists null category")
        void givenBlankCategory_whenCreateTask_thenPersistNullCategory() {
            // Arrange
            User owner = user();
            Group group = group();
            TaskRequestDTO request = new TaskRequestDTO(
                    "Prepare backend tests",
                    "Cover service behavior",
                    DEADLINE,
                    "   ",
                    GROUP_UUID
            );

            given(userRepository.findByEmailIgnoreCase(AUTHENTICATED_EMAIL)).willReturn(Optional.of(owner));
            given(groupRepository.findByUuid(GROUP_UUID)).willReturn(Optional.of(group));
            given(groupMemberRepository.findByUserIdAndGroupId(USER_ID, GROUP_ID))
                    .willReturn(Optional.of(membership(owner, group, GroupRole.ADMIN)));
            given(taskRepository.save(any(Task.class))).willAnswer(invocation -> {
                Task savedTask = invocation.getArgument(0);
                savedTask.setId(TASK_ID);
                savedTask.setUuid(TASK_UUID);
                return savedTask;
            });

            // Act
            TaskResponseDTO response = taskService.create(AUTHENTICATED_EMAIL, request);

            // Assert
            ArgumentCaptor<Task> taskCaptor = ArgumentCaptor.forClass(Task.class);
            verify(taskRepository, times(1)).save(taskCaptor.capture());

            assertThat(taskCaptor.getValue().getCategory()).isNull();
            assertThat(response.category()).isNull();
        }

        @Test
        @DisplayName("given unknown authenticated user, when creating task, then throws EntityNotFoundException")
        void givenUnknownAuthenticatedUser_whenCreateTask_thenThrowEntityNotFoundException() {
            // Arrange
            TaskRequestDTO request = validCreateRequest();
            given(userRepository.findByEmailIgnoreCase(AUTHENTICATED_EMAIL)).willReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> taskService.create(AUTHENTICATED_EMAIL, request))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessage("Authenticated user not found");

            verifyNoInteractions(groupRepository, groupMemberRepository, taskRepository);
        }

        @Test
        @DisplayName("given member role, when creating task, then denies access and does not persist")
        void givenMemberRole_whenCreateTask_thenDenyAccessAndDoNotPersist() {
            // Arrange
            User member = user();
            Group group = group();

            given(userRepository.findByEmailIgnoreCase(AUTHENTICATED_EMAIL)).willReturn(Optional.of(member));
            given(groupRepository.findByUuid(GROUP_UUID)).willReturn(Optional.of(group));
            given(groupMemberRepository.findByUserIdAndGroupId(USER_ID, GROUP_ID))
                    .willReturn(Optional.of(membership(member, group, GroupRole.MEMBER)));

            // Act & Assert
            assertThatThrownBy(() -> taskService.create(AUTHENTICATED_EMAIL, validCreateRequest()))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessage("Only group owners and admins can manage tasks");

            verify(taskRepository, never()).save(any(Task.class));
        }
    }

    @Nested
    @DisplayName("List tasks by group")
    class ListTasksByGroup {

        @Test
        @DisplayName("given owner membership, when listing tasks, then returns tasks with invitation code")
        void givenOwnerMembership_whenListByGroup_thenReturnTasksWithInvitationCode() {
            // Arrange
            User owner = user();
            Group group = group();
            Task firstTask = task("First task", DEADLINE.minusDays(1), "Planning", group, owner);
            Task secondTask = task("Second task", DEADLINE.plusDays(1), "Execution", group, owner);

            given(userRepository.findByEmailIgnoreCase(AUTHENTICATED_EMAIL)).willReturn(Optional.of(owner));
            given(groupRepository.findByUuid(GROUP_UUID)).willReturn(Optional.of(group));
            given(groupMemberRepository.findByUserIdAndGroupId(USER_ID, GROUP_ID))
                    .willReturn(Optional.of(membership(owner, group, GroupRole.OWNER)));
            given(taskRepository.findByGroupIdOrderByDeadlineAsc(GROUP_ID))
                    .willReturn(List.of(firstTask, secondTask));

            // Act
            List<TaskResponseDTO> responses = taskService.listByGroup(AUTHENTICATED_EMAIL, GROUP_UUID);

            // Assert
            assertThat(responses).hasSize(2);
            assertThat(responses)
                    .extracting(TaskResponseDTO::title)
                    .containsExactly("First task", "Second task");
            assertThat(responses)
                    .extracting(response -> response.group().invitationCode())
                    .containsExactly("INV123", "INV123");

            verify(taskRepository, times(1)).findByGroupIdOrderByDeadlineAsc(GROUP_ID);
        }

        @Test
        @DisplayName("given member membership, when listing tasks, then hides invitation code")
        void givenMemberMembership_whenListByGroup_thenHideInvitationCode() {
            // Arrange
            User member = user();
            Group group = group();

            given(userRepository.findByEmailIgnoreCase(AUTHENTICATED_EMAIL)).willReturn(Optional.of(member));
            given(groupRepository.findByUuid(GROUP_UUID)).willReturn(Optional.of(group));
            given(groupMemberRepository.findByUserIdAndGroupId(USER_ID, GROUP_ID))
                    .willReturn(Optional.of(membership(member, group, GroupRole.MEMBER)));
            given(taskRepository.findByGroupIdOrderByDeadlineAsc(GROUP_ID))
                    .willReturn(List.of(task("Visible task", DEADLINE, "Daily", group, member)));

            // Act
            List<TaskResponseDTO> responses = taskService.listByGroup(AUTHENTICATED_EMAIL, GROUP_UUID);

            // Assert
            assertThat(responses).hasSize(1);
            assertThat(responses.getFirst().group().invitationCode()).isNull();
        }

        @Test
        @DisplayName("given user without membership, when listing tasks, then throws AccessDeniedException")
        void givenUserWithoutMembership_whenListByGroup_thenThrowAccessDeniedException() {
            // Arrange
            User user = user();
            Group group = group();

            given(userRepository.findByEmailIgnoreCase(AUTHENTICATED_EMAIL)).willReturn(Optional.of(user));
            given(groupRepository.findByUuid(GROUP_UUID)).willReturn(Optional.of(group));
            given(groupMemberRepository.findByUserIdAndGroupId(USER_ID, GROUP_ID)).willReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> taskService.listByGroup(AUTHENTICATED_EMAIL, GROUP_UUID))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessage("User does not belong to this group");

            verify(taskRepository, never()).findByGroupIdOrderByDeadlineAsc(any());
        }
    }

    @Nested
    @DisplayName("Update task")
    class UpdateTask {

        @Test
        @DisplayName("given admin and valid request, when updating task, then mutates managed task and returns response")
        void givenAdminAndValidRequest_whenUpdateTask_thenMutateManagedTaskAndReturnResponse() {
            // Arrange
            User admin = user();
            Group group = group();
            Task existingTask = task("Old title", DEADLINE.minusDays(2), "Old", group, admin);
            TaskUpdateRequestDTO request = new TaskUpdateRequestDTO(
                    "  Updated title  ",
                    "  Updated description  ",
                    DEADLINE,
                    "  Quality  "
            );

            given(userRepository.findByEmailIgnoreCase(AUTHENTICATED_EMAIL)).willReturn(Optional.of(admin));
            given(taskRepository.findByUuid(TASK_UUID)).willReturn(Optional.of(existingTask));
            given(groupMemberRepository.findByUserIdAndGroupId(USER_ID, GROUP_ID))
                    .willReturn(Optional.of(membership(admin, group, GroupRole.ADMIN)));

            // Act
            TaskResponseDTO response = taskService.update(AUTHENTICATED_EMAIL, TASK_UUID, request);

            // Assert
            assertThat(existingTask.getTitle()).isEqualTo("Updated title");
            assertThat(existingTask.getDescription()).isEqualTo("Updated description");
            assertThat(existingTask.getDeadline()).isEqualTo(DEADLINE);
            assertThat(existingTask.getCategory()).isEqualTo("Quality");

            assertThat(response.uuid()).isEqualTo(TASK_UUID);
            assertThat(response.title()).isEqualTo("Updated title");
            assertThat(response.description()).isEqualTo("Updated description");
            assertThat(response.category()).isEqualTo("Quality");
            assertThat(response.group().invitationCode()).isEqualTo("INV123");

            verify(taskRepository, never()).save(any(Task.class));
        }

        @Test
        @DisplayName("given unknown task, when updating task, then throws EntityNotFoundException")
        void givenUnknownTask_whenUpdateTask_thenThrowEntityNotFoundException() {
            // Arrange
            given(userRepository.findByEmailIgnoreCase(AUTHENTICATED_EMAIL)).willReturn(Optional.of(user()));
            given(taskRepository.findByUuid(TASK_UUID)).willReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> taskService.update(AUTHENTICATED_EMAIL, TASK_UUID, validUpdateRequest()))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessage("Task not found");

            verifyNoInteractions(groupMemberRepository);
            verify(taskRepository, never()).save(any(Task.class));
        }

        @Test
        @DisplayName("given member role, when updating task, then denies access and leaves task unchanged")
        void givenMemberRole_whenUpdateTask_thenDenyAccessAndLeaveTaskUnchanged() {
            // Arrange
            User member = user();
            Group group = group();
            Task existingTask = task("Original title", DEADLINE.minusDays(2), "Original", group, member);

            given(userRepository.findByEmailIgnoreCase(AUTHENTICATED_EMAIL)).willReturn(Optional.of(member));
            given(taskRepository.findByUuid(TASK_UUID)).willReturn(Optional.of(existingTask));
            given(groupMemberRepository.findByUserIdAndGroupId(USER_ID, GROUP_ID))
                    .willReturn(Optional.of(membership(member, group, GroupRole.MEMBER)));

            // Act & Assert
            assertThatThrownBy(() -> taskService.update(AUTHENTICATED_EMAIL, TASK_UUID, validUpdateRequest()))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessage("Only group owners and admins can manage tasks");

            assertThat(existingTask.getTitle()).isEqualTo("Original title");
            assertThat(existingTask.getDescription()).isEqualTo("Original title description");
            assertThat(existingTask.getCategory()).isEqualTo("Original");
            verify(taskRepository, never()).save(any(Task.class));
        }
    }

    @Nested
    @DisplayName("Delete task")
    class DeleteTask {

        @Test
        @DisplayName("given owner and existing task, when deleting task, then deletes repository entity")
        void givenOwnerAndExistingTask_whenDeleteTask_thenDeleteRepositoryEntity() {
            // Arrange
            User owner = user();
            Group group = group();
            Task existingTask = task("Task to delete", DEADLINE, "Cleanup", group, owner);

            given(userRepository.findByEmailIgnoreCase(AUTHENTICATED_EMAIL)).willReturn(Optional.of(owner));
            given(taskRepository.findByUuid(TASK_UUID)).willReturn(Optional.of(existingTask));
            given(groupMemberRepository.findByUserIdAndGroupId(USER_ID, GROUP_ID))
                    .willReturn(Optional.of(membership(owner, group, GroupRole.OWNER)));

            // Act
            taskService.delete(AUTHENTICATED_EMAIL, TASK_UUID);

            // Assert
            verify(taskRepository, times(1)).delete(existingTask);
        }

        @Test
        @DisplayName("given unauthorized member, when deleting task, then throws AccessDeniedException and does not delete")
        void givenUnauthorizedMember_whenDeleteTask_thenThrowAccessDeniedExceptionAndDoNotDelete() {
            // Arrange
            User member = user();
            Group group = group();
            Task existingTask = task("Protected task", DEADLINE, "Security", group, member);

            given(userRepository.findByEmailIgnoreCase(AUTHENTICATED_EMAIL)).willReturn(Optional.of(member));
            given(taskRepository.findByUuid(TASK_UUID)).willReturn(Optional.of(existingTask));
            given(groupMemberRepository.findByUserIdAndGroupId(USER_ID, GROUP_ID))
                    .willReturn(Optional.of(membership(member, group, GroupRole.MEMBER)));

            // Act & Assert
            assertThatThrownBy(() -> taskService.delete(AUTHENTICATED_EMAIL, TASK_UUID))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessage("Only group owners and admins can manage tasks");

            verify(taskRepository, never()).delete(any(Task.class));
        }
    }

    private static TaskRequestDTO validCreateRequest() {
        return new TaskRequestDTO(
                "Prepare backend tests",
                "Cover service behavior",
                DEADLINE,
                "Quality",
                GROUP_UUID
        );
    }

    private static TaskUpdateRequestDTO validUpdateRequest() {
        return new TaskUpdateRequestDTO(
                "Updated title",
                "Updated description",
                DEADLINE,
                "Quality"
        );
    }

    private static User user() {
        User user = new User();
        user.setId(USER_ID);
        user.setUuid(USER_UUID);
        user.setName("Task Owner");
        user.setEmail(AUTHENTICATED_EMAIL);
        user.setUsername("taskowner");
        user.setPassword("encoded-password");
        return user;
    }

    private static Group group() {
        Group group = new Group();
        group.setId(GROUP_ID);
        group.setUuid(GROUP_UUID);
        group.setName("Backend Team");
        group.setInvitationCode("INV123");
        return group;
    }

    private static GroupMember membership(User user, Group group, GroupRole role) {
        GroupMember membership = new GroupMember();
        membership.setId(40L);
        membership.setUser(user);
        membership.setGroup(group);
        membership.setGroupRole(role);
        return membership;
    }

    private static Task task(String title, LocalDateTime deadline, String category, Group group, User createdBy) {
        Task task = new Task();
        task.setId(TASK_ID);
        task.setUuid(TASK_UUID);
        task.setTitle(title);
        task.setDescription(title + " description");
        task.setDeadline(deadline);
        task.setCategory(category);
        task.setGroup(group);
        task.setCreatedBy(createdBy);
        return task;
    }
}
