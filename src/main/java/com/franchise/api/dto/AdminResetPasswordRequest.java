package com.franchise.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdminResetPasswordRequest(
        @NotBlank
        @Size(min = 8, message = "La contrasena debe tener al menos 8 caracteres")
        String newPassword
) {
}
