package com.franchise.api.dto;

import java.util.List;

public record BranchResponse(String id, String name, List<ProductResponse> products) {
}
