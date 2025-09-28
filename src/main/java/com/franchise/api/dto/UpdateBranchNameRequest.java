package com.franchise.api.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateBranchNameRequest(@NotBlank String name) {
}
