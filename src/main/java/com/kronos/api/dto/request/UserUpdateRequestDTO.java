package com.kronos.api.dto.request;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UserUpdateRequestDTO(
        @Size(min = 3, max = 100, message = "Name must be between 3 and 100 characters")
        String name,

        @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
        @Pattern(regexp = "^[A-Za-z0-9._-]+$", message = "Username can only contain letters, numbers, dots, underscores and hyphens")
        String username
) {

    @AssertTrue(message = "At least one user field must be provided")
    public boolean isAtLeastOneFieldPresent() {
        return hasText(name) || hasText(username);
    }

    @AssertTrue(message = "Name cannot be blank")
    public boolean isNameValidWhenPresent() {
        return name == null || hasText(name);
    }

    @AssertTrue(message = "Username cannot be blank")
    public boolean isUsernameValidWhenPresent() {
        return username == null || hasText(username);
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
