package com.kronos.api.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

public record TaskResponseDTO(
        UUID uuid,
        String title,
        String description,
        LocalDateTime deadline,
        String category,
        GroupResponseDTO group,
        UserResponseDTO createdBy
) {
}
