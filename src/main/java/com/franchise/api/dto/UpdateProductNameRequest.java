package com.franchise.api.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateProductNameRequest(@NotBlank String name) {
}
