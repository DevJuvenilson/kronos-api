package com.kronos.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record GroupMemberAddRequestDTO(
        @NotBlank(message = "User identifier cannot be blank")
        @Size(max = 150, message = "User identifier must not exceed 150 characters")
        String userIdentifier
) {
}
