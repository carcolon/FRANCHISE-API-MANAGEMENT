package com.franchise.api.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateBranchRequest(@NotBlank String name) {
}
