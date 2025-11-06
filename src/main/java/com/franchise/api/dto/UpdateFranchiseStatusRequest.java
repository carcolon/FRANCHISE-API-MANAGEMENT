package com.franchise.api.dto;

import jakarta.validation.constraints.NotNull;

public record UpdateFranchiseStatusRequest(@NotNull Boolean active) {
}
