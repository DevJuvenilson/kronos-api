package com.kronos.api.dto.response;

public record LoginResponseDTO(
        String token,
        String tokenType,
        UserResponseDTO user
) {
}
