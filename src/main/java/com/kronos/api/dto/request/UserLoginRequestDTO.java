package com.kronos.api.dto.request;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UserLoginRequestDTO(
        @Email(message = "Invalid email format")
        @Size(max = 150, message = "Email must not exceed 150 characters")
        String email,

        @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
        @Pattern(regexp = "^[A-Za-z0-9._-]+$", message = "Username can only contain letters, numbers, dots, underscores and hyphens")
        String username,

        @NotBlank(message = "Password cannot be empty")
        @Size(min = 8, message = "Password must be at least 8 characters long")
        String password
) {

    @AssertTrue(message = "Email or username must be provided")
    public boolean isLoginIdentifierPresent() {
        return hasText(email) || hasText(username);
    }

    @AssertTrue(message = "Provide either email or username, not both")
    public boolean isSingleLoginIdentifierProvided() {
        return hasText(email) ^ hasText(username);
    }

    @AssertTrue(message = "Email cannot be blank")
    public boolean isEmailValidWhenPresent() {
        return email == null || hasText(email);
    }

    @AssertTrue(message = "Username cannot be blank")
    public boolean isUsernameValidWhenPresent() {
        return username == null || hasText(username);
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
