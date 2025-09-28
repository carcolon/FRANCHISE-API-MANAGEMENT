package com.franchise.api.dto;

public record TopProductPerBranchResponse(String branchId, String branchName, ProductResponse product) {
}
