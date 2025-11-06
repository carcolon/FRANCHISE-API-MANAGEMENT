package com.franchise.api.dto;

import java.util.List;

public record BranchResponse(String id, String name, boolean active, List<ProductResponse> products) {
}
