package com.kronos.api.dto.response;

import java.util.UUID;

public record UserResponseDTO(
        UUID uuid,
        String name,
        String email,
        String username
) {
}
