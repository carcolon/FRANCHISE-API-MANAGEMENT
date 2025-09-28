package com.franchise.api.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateFranchiseNameRequest(@NotBlank String name) {
}
