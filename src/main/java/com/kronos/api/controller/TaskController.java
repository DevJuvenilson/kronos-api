package com.kronos.api.controller;

import com.kronos.api.dto.request.TaskRequestDTO;
import com.kronos.api.dto.request.TaskUpdateRequestDTO;
import com.kronos.api.dto.response.TaskResponseDTO;
import com.kronos.api.infra.exception.CustomErrorResponse;
import com.kronos.api.service.TaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
@Tag(
        name = "Tasks",
        description = "Operations for creating, listing, updating, and deleting tasks within groups."
)
public class TaskController {

    private final TaskService taskService;

    /**
     * Creates a task inside a group visible to the authenticated user.
     *
     * <p>Only group owners and admins are allowed to create tasks. Validation
     * constraints from {@link TaskRequestDTO} are reflected in the generated
     * OpenAPI schema.</p>
     */
    @Operation(
            summary = "Create a task",
            description = "Creates a new task for a group. The authenticated user must be a group owner or admin."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Task created successfully.",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = TaskResponseDTO.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Malformed request body or invalid JSON.",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = CustomErrorResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Authenticated user does not have permission to manage tasks in this group.",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = CustomErrorResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Authenticated user or target group was not found.",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = CustomErrorResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "422",
                    description = "Request body failed validation.",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = CustomErrorResponse.class)
                    )
            )
    })
    @PostMapping
    public ResponseEntity<TaskResponseDTO> create(
            @Parameter(hidden = true)
            @AuthenticationPrincipal UserDetails authenticatedUser,
            @RequestBody @Valid TaskRequestDTO request
    ) {
        TaskResponseDTO response = taskService.create(authenticatedUser.getUsername(), request);
        return ResponseEntity.status(HttpStatusCode.valueOf(201)).body(response);
    }

    /**
     * Lists tasks that belong to a specific group.
     *
     * <p>Members can view tasks for groups they belong to. Invitation code
     * visibility is controlled by the service layer according to the user's
     * group role.</p>
     */
    @Operation(
            summary = "List tasks by group",
            description = "Returns all tasks for the provided group UUID ordered by deadline."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Tasks returned successfully.",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            array = @ArraySchema(schema = @Schema(implementation = TaskResponseDTO.class))
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Authenticated user does not belong to the group.",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = CustomErrorResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Authenticated user or group was not found.",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = CustomErrorResponse.class)
                    )
            )
    })
    @GetMapping("/group/{groupUuid}")
    public ResponseEntity<List<TaskResponseDTO>> listByGroup(
            @Parameter(hidden = true)
            @AuthenticationPrincipal UserDetails authenticatedUser,
            @Parameter(
                    description = "Public UUID of the group whose tasks should be listed.",
                    example = "22222222-2222-2222-2222-222222222222",
                    required = true
            )
            @PathVariable UUID groupUuid
    ) {
        List<TaskResponseDTO> response = taskService.listByGroup(authenticatedUser.getUsername(), groupUuid);
        return ResponseEntity.ok(response);
    }

    /**
     * Updates a task identified by its public UUID.
     *
     * <p>The endpoint replaces the mutable task fields with the validated
     * request payload. Only group owners and admins may perform this operation.</p>
     */
    @Operation(
            summary = "Update a task",
            description = "Updates title, description, deadline, and category for an existing task."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Task updated successfully.",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = TaskResponseDTO.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Malformed request body, invalid JSON, or invalid UUID format.",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = CustomErrorResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Authenticated user does not have permission to manage this task.",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = CustomErrorResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Authenticated user or target task was not found.",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = CustomErrorResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "422",
                    description = "Request body failed validation.",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = CustomErrorResponse.class)
                    )
            )
    })
    @PutMapping("/{taskUuid}")
    public ResponseEntity<TaskResponseDTO> update(
            @Parameter(hidden = true)
            @AuthenticationPrincipal UserDetails authenticatedUser,
            @Parameter(
                    description = "Public UUID of the task to update.",
                    example = "11111111-1111-1111-1111-111111111111",
                    required = true
            )
            @PathVariable UUID taskUuid,
            @RequestBody @Valid TaskUpdateRequestDTO request
    ) {
        TaskResponseDTO response = taskService.update(authenticatedUser.getUsername(), taskUuid, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Deletes a task identified by its public UUID.
     *
     * <p>The operation is idempotent from an HTTP contract perspective only
     * after the task exists and the authenticated user has management privileges.</p>
     */
    @Operation(
            summary = "Delete a task",
            description = "Deletes an existing task. The authenticated user must be a group owner or admin."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Task deleted successfully.", content = @Content),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid UUID format.",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = CustomErrorResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Authenticated user does not have permission to delete this task.",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = CustomErrorResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Authenticated user or target task was not found.",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = CustomErrorResponse.class)
                    )
            )
    })
    @DeleteMapping("/{taskUuid}")
    public ResponseEntity<Void> delete(
            @Parameter(hidden = true)
            @AuthenticationPrincipal UserDetails authenticatedUser,
            @Parameter(
                    description = "Public UUID of the task to delete.",
                    example = "11111111-1111-1111-1111-111111111111",
                    required = true
            )
            @PathVariable UUID taskUuid
    ) {
        taskService.delete(authenticatedUser.getUsername(), taskUuid);
        return ResponseEntity.noContent().build();
    }
}
