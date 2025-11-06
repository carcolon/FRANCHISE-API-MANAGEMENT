package com.franchise.api.controller;

import com.franchise.api.dto.BranchResponse;
import com.franchise.api.dto.CreateBranchRequest;
import com.franchise.api.dto.CreateFranchiseRequest;
import com.franchise.api.dto.CreateProductRequest;
import com.franchise.api.dto.FranchiseResponse;
import com.franchise.api.dto.ProductResponse;
import com.franchise.api.dto.TopProductPerBranchResponse;
import com.franchise.api.dto.UpdateBranchNameRequest;
import com.franchise.api.dto.UpdateBranchStatusRequest;
import com.franchise.api.dto.UpdateFranchiseNameRequest;
import com.franchise.api.dto.UpdateFranchiseStatusRequest;
import com.franchise.api.dto.UpdateProductNameRequest;
import com.franchise.api.dto.UpdateProductStockRequest;
import com.franchise.api.service.FranchiseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/franchises")
@RequiredArgsConstructor
public class FranchiseController {

    private final FranchiseService franchiseService;

    @PostMapping
    public ResponseEntity<FranchiseResponse> createFranchise(@Valid @RequestBody CreateFranchiseRequest request) {
        FranchiseResponse response = franchiseService.createFranchise(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.id())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping
    public ResponseEntity<List<FranchiseResponse>> getFranchises() {
        return ResponseEntity.ok(franchiseService.getAllFranchises());
    }

    @GetMapping("/{franchiseId}")
    public ResponseEntity<FranchiseResponse> getFranchise(@PathVariable String franchiseId) {
        return ResponseEntity.ok(franchiseService.getFranchiseById(franchiseId));
    }

    @PatchMapping("/{franchiseId}")
    public ResponseEntity<FranchiseResponse> updateFranchiseName(@PathVariable String franchiseId,
                                                                 @Valid @RequestBody UpdateFranchiseNameRequest request) {
        return ResponseEntity.ok(franchiseService.updateFranchiseName(franchiseId, request));
    }

    @PatchMapping("/{franchiseId}/status")
    public ResponseEntity<FranchiseResponse> updateFranchiseStatus(@PathVariable String franchiseId,
                                                                   @Valid @RequestBody UpdateFranchiseStatusRequest request) {
        return ResponseEntity.ok(franchiseService.updateFranchiseStatus(franchiseId, request));
    }

    @DeleteMapping("/{franchiseId}")
    public ResponseEntity<Void> deleteFranchise(@PathVariable String franchiseId) {
        franchiseService.deleteFranchise(franchiseId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{franchiseId}/branches")
    public ResponseEntity<BranchResponse> addBranch(@PathVariable String franchiseId,
                                                    @Valid @RequestBody CreateBranchRequest request) {
        BranchResponse response = franchiseService.addBranch(franchiseId, request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{branchId}")
                .buildAndExpand(response.id())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @PatchMapping("/{franchiseId}/branches/{branchId}")
    public ResponseEntity<BranchResponse> updateBranchName(@PathVariable String franchiseId,
                                                           @PathVariable String branchId,
                                                           @Valid @RequestBody UpdateBranchNameRequest request) {
        return ResponseEntity.ok(franchiseService.updateBranchName(franchiseId, branchId, request));
    }

    @PatchMapping("/{franchiseId}/branches/{branchId}/status")
    public ResponseEntity<BranchResponse> updateBranchStatus(@PathVariable String franchiseId,
                                                             @PathVariable String branchId,
                                                             @Valid @RequestBody UpdateBranchStatusRequest request) {
        return ResponseEntity.ok(franchiseService.updateBranchStatus(franchiseId, branchId, request));
    }

    @DeleteMapping("/{franchiseId}/branches/{branchId}")
    public ResponseEntity<Void> deleteBranch(@PathVariable String franchiseId,
                                             @PathVariable String branchId) {
        franchiseService.deleteBranch(franchiseId, branchId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{franchiseId}/branches/{branchId}/products")
    public ResponseEntity<ProductResponse> addProduct(@PathVariable String franchiseId,
                                                       @PathVariable String branchId,
                                                       @Valid @RequestBody CreateProductRequest request) {
        ProductResponse response = franchiseService.addProduct(franchiseId, branchId, request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{productId}")
                .buildAndExpand(response.id())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @PatchMapping("/{franchiseId}/branches/{branchId}/products/{productId}/stock")
    public ResponseEntity<ProductResponse> updateProductStock(@PathVariable String franchiseId,
                                                              @PathVariable String branchId,
                                                              @PathVariable String productId,
                                                              @Valid @RequestBody UpdateProductStockRequest request) {
        return ResponseEntity.ok(franchiseService.updateProductStock(franchiseId, branchId, productId, request));
    }

    @PatchMapping("/{franchiseId}/branches/{branchId}/products/{productId}")
    public ResponseEntity<ProductResponse> updateProductName(@PathVariable String franchiseId,
                                                              @PathVariable String branchId,
                                                              @PathVariable String productId,
                                                              @Valid @RequestBody UpdateProductNameRequest request) {
        return ResponseEntity.ok(franchiseService.updateProductName(franchiseId, branchId, productId, request));
    }

    @DeleteMapping("/{franchiseId}/branches/{branchId}/products/{productId}")
    public ResponseEntity<Void> deleteProduct(@PathVariable String franchiseId,
                                              @PathVariable String branchId,
                                              @PathVariable String productId) {
        franchiseService.deleteProduct(franchiseId, branchId, productId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{franchiseId}/branches/top-products")
    public ResponseEntity<List<TopProductPerBranchResponse>> getTopProductPerBranch(@PathVariable String franchiseId) {
        return ResponseEntity.ok(franchiseService.getTopProductPerBranch(franchiseId));
    }
}
