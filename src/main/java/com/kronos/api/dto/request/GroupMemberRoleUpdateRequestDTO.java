package com.kronos.api.dto.request;

import com.kronos.api.model.enums.GroupRole;
import jakarta.validation.constraints.NotNull;

public record GroupMemberRoleUpdateRequestDTO(
        @NotNull(message = "Group role cannot be null")
        GroupRole groupRole
) {
}
