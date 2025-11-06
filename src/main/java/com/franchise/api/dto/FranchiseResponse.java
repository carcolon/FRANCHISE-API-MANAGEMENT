package com.franchise.api.dto;

import java.util.List;

public record FranchiseResponse(String id, String name, boolean active, List<BranchResponse> branches) {
}
