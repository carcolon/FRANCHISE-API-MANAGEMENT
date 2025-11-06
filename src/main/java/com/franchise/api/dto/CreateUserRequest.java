package com.franchise.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Set;

public record CreateUserRequest(
        @NotBlank
        @Size(min = 3, max = 40)
        String username,
        @NotBlank
        @Size(min = 8, message = "La contrasena debe tener al menos 8 caracteres")
        String password,
        @NotBlank
        @Size(min = 3, max = 80)
        String fullName,
        @NotBlank
        @Email
        String email,
        Set<String> roles
) {
}
