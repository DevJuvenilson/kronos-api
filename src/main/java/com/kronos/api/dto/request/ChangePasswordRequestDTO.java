package com.kronos.api.dto.request;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequestDTO(
        @NotBlank(message = "Current password cannot be empty")
        String currentPassword,

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
