package com.kronos.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record GroupRequestDTO(
        @NotBlank(message = "Group name cannot be blank")
        @Size(min = 3, max = 100, message = "Group name must be between 3 and 100 characters")
        String name
) {
}
