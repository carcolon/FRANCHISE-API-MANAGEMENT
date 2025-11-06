package com.franchise.api.dto;

import jakarta.validation.constraints.NotNull;

public record UpdateBranchStatusRequest(@NotNull Boolean active) {
}
