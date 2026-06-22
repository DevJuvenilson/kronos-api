package com.kronos.api.dto.request;

import com.kronos.api.model.enums.GroupRole;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record GroupMemberRequestDTO(
        @NotNull(message = "User UUID cannot be null")
        UUID userUuid,

        @NotNull(message = "Group UUID cannot be null")
        UUID groupUuid,

        @NotNull(message = "Group role cannot be null")
        GroupRole groupRole
) {
}
