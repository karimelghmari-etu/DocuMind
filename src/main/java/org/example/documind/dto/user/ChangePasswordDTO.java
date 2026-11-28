package org.example.documind.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/** The change-password form on the Settings page. Same 8-character rule as registration. */
@Getter
@Setter
public class ChangePasswordDTO {
    @NotBlank(message = "Enter your current password")
    private String currentPassword;

    @NotBlank(message = "Enter a new password")
    @Size(min = 8, message = "Password must contain at least 8 characters")
    private String newPassword;

    @NotBlank(message = "Confirm your new password")
    private String confirmPassword;
}
