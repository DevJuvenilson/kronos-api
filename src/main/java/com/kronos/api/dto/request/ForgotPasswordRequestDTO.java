package com.kronos.api.dto.request;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ForgotPasswordRequestDTO(
        @NotBlank(message = "Email cannot be empty")
        @Email(message = "Invalid email format")
        @Size(max = 150, message = "Email must not exceed 150 characters")
        String email,

        @NotBlank(message = "New password cannot be empty")
        @Size(min = 8, message = "New password must be at least 8 characters long")
        String newPassword,

        @NotBlank(message = "Password confirmation cannot be empty")
        String confirmNewPassword
) {

    @AssertTrue(message = "Password confirmation does not match")
    public boolean isPasswordConfirmationValid() {
        return newPassword != null && newPassword.equals(confirmNewPassword);
    }
}
