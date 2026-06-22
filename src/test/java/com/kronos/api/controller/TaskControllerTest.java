package com.kronos.api.controller;

import com.kronos.api.dto.request.TaskRequestDTO;
import com.kronos.api.dto.request.TaskUpdateRequestDTO;
import com.kronos.api.dto.response.GroupResponseDTO;
import com.kronos.api.dto.response.TaskResponseDTO;
import com.kronos.api.dto.response.UserResponseDTO;
import com.kronos.api.infra.exception.GlobalExceptionHandler;
import com.kronos.api.infra.security.JwtAuthenticationFilter;
import com.kronos.api.service.TaskService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = TaskController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = JwtAuthenticationFilter.class
        )
)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
@WithMockUser(username = TaskControllerTest.AUTHENTICATED_EMAIL)
class TaskControllerTest {

    private static final String BASE_URL = "/api/tasks";
    static final String AUTHENTICATED_EMAIL = "owner@example.com";

    private static final UUID TASK_UUID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID GROUP_UUID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID USER_UUID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final LocalDateTime DEADLINE = LocalDateTime.of(2026, 7, 1, 9, 30, 15);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JsonMapper jsonMapper;

    @MockitoBean
    private TaskService taskService;

    @Test
    @DisplayName("POST /api/tasks - given valid request, when creating task, then returns 201 Created")
    void givenValidRequest_whenCreateTask_thenReturnCreatedTaskAndStatus201() throws Exception {
        // Arrange
        TaskRequestDTO request = validCreateRequest();
        TaskResponseDTO response = taskResponse(
                TASK_UUID,
                "Prepare backend tests",
                "Cover REST controller scenarios",
                "Testing"
        );

        given(taskService.create(eq(AUTHENTICATED_EMAIL), any(TaskRequestDTO.class))).willReturn(response);

        // Act & Assert
        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.uuid").value(TASK_UUID.toString()))
                .andExpect(jsonPath("$.title").value("Prepare backend tests"))
                .andExpect(jsonPath("$.description").value("Cover REST controller scenarios"))
                .andExpect(jsonPath("$.deadline").value("2026-07-01T09:30:15"))
                .andExpect(jsonPath("$.category").value("Testing"))
                .andExpect(jsonPath("$.group.uuid").value(GROUP_UUID.toString()))
                .andExpect(jsonPath("$.group.name").value("Backend Team"))
                .andExpect(jsonPath("$.createdBy.uuid").value(USER_UUID.toString()))
                .andExpect(jsonPath("$.createdBy.email").value(AUTHENTICATED_EMAIL));

        ArgumentCaptor<TaskRequestDTO> requestCaptor = ArgumentCaptor.forClass(TaskRequestDTO.class);
        verify(taskService, times(1)).create(eq(AUTHENTICATED_EMAIL), requestCaptor.capture());

        TaskRequestDTO capturedRequest = requestCaptor.getValue();
        assertThat(capturedRequest.title()).isEqualTo("Prepare backend tests");
        assertThat(capturedRequest.description()).isEqualTo("Cover REST controller scenarios");
        assertThat(capturedRequest.deadline()).isEqualTo(DEADLINE);
        assertThat(capturedRequest.category()).isEqualTo("Testing");
        assertThat(capturedRequest.groupUuid()).isEqualTo(GROUP_UUID);
    }

    @Test
    @DisplayName("GET /api/tasks/group/{groupUuid} - given valid group, when listing tasks, then returns 200 OK")
    void givenValidGroupUuid_whenListTasksByGroup_thenReturnTaskListAndStatus200() throws Exception {
        // Arrange
        TaskResponseDTO firstTask = taskResponse(
                TASK_UUID,
                "Prepare backend tests",
                "Cover REST controller scenarios",
                "Testing"
        );
        TaskResponseDTO secondTask = taskResponse(
                UUID.fromString("44444444-4444-4444-4444-444444444444"),
                "Review pull request",
                "Validate controller test coverage",
                "Code Review"
        );

        given(taskService.listByGroup(AUTHENTICATED_EMAIL, GROUP_UUID))
                .willReturn(List.of(firstTask, secondTask));

        // Act & Assert
        mockMvc.perform(get(BASE_URL + "/group/{groupUuid}", GROUP_UUID)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].uuid").value(TASK_UUID.toString()))
                .andExpect(jsonPath("$[0].title").value("Prepare backend tests"))
                .andExpect(jsonPath("$[1].uuid").value("44444444-4444-4444-4444-444444444444"))
                .andExpect(jsonPath("$[1].title").value("Review pull request"));

        verify(taskService, times(1)).listByGroup(AUTHENTICATED_EMAIL, GROUP_UUID);
    }

    @Test
    @DisplayName("PUT /api/tasks/{taskUuid} - given valid request, when updating task, then returns 200 OK")
    void givenValidRequest_whenUpdateTask_thenReturnUpdatedTaskAndStatus200() throws Exception {
        // Arrange
        TaskUpdateRequestDTO request = validUpdateRequest();
        TaskResponseDTO response = taskResponse(
                TASK_UUID,
                "Update backend tests",
                "Add validation and exception coverage",
                "Quality"
        );

        given(taskService.update(eq(AUTHENTICATED_EMAIL), eq(TASK_UUID), any(TaskUpdateRequestDTO.class)))
                .willReturn(response);

        // Act & Assert
        mockMvc.perform(put(BASE_URL + "/{taskUuid}", TASK_UUID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.uuid").value(TASK_UUID.toString()))
                .andExpect(jsonPath("$.title").value("Update backend tests"))
                .andExpect(jsonPath("$.description").value("Add validation and exception coverage"))
                .andExpect(jsonPath("$.category").value("Quality"));

        ArgumentCaptor<TaskUpdateRequestDTO> requestCaptor = ArgumentCaptor.forClass(TaskUpdateRequestDTO.class);
        verify(taskService, times(1)).update(eq(AUTHENTICATED_EMAIL), eq(TASK_UUID), requestCaptor.capture());

        TaskUpdateRequestDTO capturedRequest = requestCaptor.getValue();
        assertThat(capturedRequest.title()).isEqualTo("Update backend tests");
        assertThat(capturedRequest.description()).isEqualTo("Add validation and exception coverage");
        assertThat(capturedRequest.deadline()).isEqualTo(DEADLINE);
        assertThat(capturedRequest.category()).isEqualTo("Quality");
    }

    @Test
    @DisplayName("DELETE /api/tasks/{taskUuid} - given existing task, when deleting task, then returns 204 No Content")
    void givenExistingTaskUuid_whenDeleteTask_thenReturnStatus204() throws Exception {
        // Arrange

        // Act & Assert
        mockMvc.perform(delete(BASE_URL + "/{taskUuid}", TASK_UUID))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(taskService, times(1)).delete(AUTHENTICATED_EMAIL, TASK_UUID);
    }

    @Test
    @DisplayName("GET /api/tasks/group/{groupUuid} - given unknown group, when listing tasks, then returns 404 Not Found")
    void givenUnknownGroupUuid_whenListTasksByGroup_thenReturnErrorPayloadAndStatus404() throws Exception {
        // Arrange
        given(taskService.listByGroup(AUTHENTICATED_EMAIL, GROUP_UUID))
                .willThrow(new EntityNotFoundException("Group not found"));

        // Act & Assert
        mockMvc.perform(get(BASE_URL + "/group/{groupUuid}", GROUP_UUID)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Resource Not Found"))
                .andExpect(jsonPath("$.message").value("Group not found"))
                .andExpect(jsonPath("$.path").value(BASE_URL + "/group/" + GROUP_UUID));

        verify(taskService, times(1)).listByGroup(AUTHENTICATED_EMAIL, GROUP_UUID);
    }

    @Test
    @DisplayName("POST /api/tasks - given invalid request, when validation fails, then returns 422 Unprocessable Entity")
    void givenInvalidRequest_whenCreateTask_thenReturnValidationErrorPayloadAndStatus422() throws Exception {
        // Arrange
        TaskRequestDTO invalidRequest = new TaskRequestDTO(
                "",
                "",
                null,
                "Testing",
                null
        );

        // Act & Assert
        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(invalidRequest)))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.error").value("Validation Error"))
                .andExpect(jsonPath("$.message", allOf(
                        containsString("title: Title cannot be empty"),
                        containsString("description: Description cannot be empty"),
                        containsString("deadline: Deadline cannot be null"),
                        containsString("groupUuid: Group UUID cannot be null")
                )))
                .andExpect(jsonPath("$.path").value(BASE_URL));

        verify(taskService, never()).create(any(), any(TaskRequestDTO.class));
    }

    @Test
    @DisplayName("PUT /api/tasks/{taskUuid} - given unauthorized member, when updating task, then returns 403 Forbidden")
    void givenUnauthorizedMember_whenUpdateTask_thenReturnErrorPayloadAndStatus403() throws Exception {
        // Arrange
        TaskUpdateRequestDTO request = validUpdateRequest();

        given(taskService.update(eq(AUTHENTICATED_EMAIL), eq(TASK_UUID), any(TaskUpdateRequestDTO.class)))
                .willThrow(new AccessDeniedException("Only group owners and admins can manage tasks"));

        // Act & Assert
        mockMvc.perform(put(BASE_URL + "/{taskUuid}", TASK_UUID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"))
                .andExpect(jsonPath("$.message").value("Only group owners and admins can manage tasks"))
                .andExpect(jsonPath("$.path").value(BASE_URL + "/" + TASK_UUID));

        verify(taskService, times(1)).update(eq(AUTHENTICATED_EMAIL), eq(TASK_UUID), any(TaskUpdateRequestDTO.class));
    }

    private static TaskRequestDTO validCreateRequest() {
        return new TaskRequestDTO(
                "Prepare backend tests",
                "Cover REST controller scenarios",
                DEADLINE,
                "Testing",
                GROUP_UUID
        );
    }

    private static TaskUpdateRequestDTO validUpdateRequest() {
        return new TaskUpdateRequestDTO(
                "Update backend tests",
                "Add validation and exception coverage",
                DEADLINE,
                "Quality"
        );
    }

    private static TaskResponseDTO taskResponse(UUID taskUuid, String title, String description, String category) {
        return new TaskResponseDTO(
                taskUuid,
                title,
                description,
                DEADLINE,
                category,
                new GroupResponseDTO(GROUP_UUID, "Backend Team", "INVITE123"),
                new UserResponseDTO(USER_UUID, "Task Owner", AUTHENTICATED_EMAIL, "taskowner")
        );
    }
}
