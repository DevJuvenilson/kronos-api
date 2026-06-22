package com.kronos.api.dto.response;

import java.util.UUID;

public record GroupResponseDTO(
        UUID uuid,
        String name,
        String invitationCode
) {
}
