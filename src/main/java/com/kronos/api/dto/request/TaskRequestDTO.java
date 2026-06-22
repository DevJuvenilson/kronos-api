package com.kronos.api.dto.request;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.UUID;

public record TaskRequestDTO(
        @NotBlank(message = "Title cannot be empty")
        @Size(max = 100, message = "Title must not exceed 100 characters")
        String title,

        @NotBlank(message = "Description cannot be empty")
        String description,

        @NotNull(message = "Deadline cannot be null")
        @FutureOrPresent(message = "Deadline must be in the future or present")
        LocalDateTime deadline,

        @Size(max = 50, message = "Category must not exceed 50 characters")
        String category,

        @NotNull(message = "Group UUID cannot be null")
        UUID groupUuid
) {
}
