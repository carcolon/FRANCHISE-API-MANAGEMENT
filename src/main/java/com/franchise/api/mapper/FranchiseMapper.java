package com.franchise.api.mapper;

import com.franchise.api.domain.Branch;
import com.franchise.api.domain.Franchise;
import com.franchise.api.domain.Product;
import com.franchise.api.dto.BranchResponse;
import com.franchise.api.dto.FranchiseResponse;
import com.franchise.api.dto.ProductResponse;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public final class FranchiseMapper {

    private FranchiseMapper() {
    }

    public static FranchiseResponse toResponse(Franchise franchise) {
        if (franchise == null) {
            return null;
        }
        return new FranchiseResponse(
                franchise.getId(),
                franchise.getName(),
                franchise.getActive() == null || Boolean.TRUE.equals(franchise.getActive()),
                toBranchResponses(franchise.getBranches())
        );
    }

    public static BranchResponse toBranchResponse(Branch branch) {
        if (branch == null) {
            return null;
        }
        return new BranchResponse(
                branch.getId(),
                branch.getName(),
                branch.isActive(),
                toProductResponses(branch.getProducts())
        );
    }

    public static ProductResponse toProductResponse(Product product) {
        if (product == null) {
            return null;
        }
        return new ProductResponse(product.getId(), product.getName(), product.getStock());
    }

    private static List<BranchResponse> toBranchResponses(List<Branch> branches) {
        if (branches == null) {
            return Collections.emptyList();
        }
        return branches.stream()
                .filter(Objects::nonNull)
                .map(FranchiseMapper::toBranchResponse)
                .collect(Collectors.toList());
    }

    private static List<ProductResponse> toProductResponses(List<Product> products) {
        if (products == null) {
            return Collections.emptyList();
        }
        return products.stream()
                .filter(Objects::nonNull)
                .map(FranchiseMapper::toProductResponse)
                .collect(Collectors.toList());
    }
}
