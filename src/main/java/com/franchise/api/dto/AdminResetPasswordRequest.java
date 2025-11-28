package com.franchise.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdminResetPasswordRequest(
<<<<<<< HEAD
        @NotBlank
        @Size(min = 8, message = "La contrasena debe tener al menos 8 caracteres")
=======
        @NotBlank(message = "La nueva contrasena es obligatoria")
        @Size(min = 8, message = "La nueva contrasena debe tener al menos 8 caracteres")
>>>>>>> 5c1ca9c (feat: admin reset y perfiles front/back sincronizados)
        String newPassword
) {
}
