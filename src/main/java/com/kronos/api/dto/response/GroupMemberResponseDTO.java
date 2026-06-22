package com.kronos.api.dto.response;

import com.kronos.api.model.enums.GroupRole;

public record GroupMemberResponseDTO(
        UserResponseDTO user,
        GroupResponseDTO group,
        GroupRole groupRole
) {
}
