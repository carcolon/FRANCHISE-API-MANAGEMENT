package com.franchise.api.service;

import com.franchise.api.domain.Branch;
import com.franchise.api.domain.Franchise;
import com.franchise.api.domain.Product;
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
import com.franchise.api.exception.BadRequestException;
import com.franchise.api.exception.ConflictException;
import com.franchise.api.exception.ResourceNotFoundException;
import com.franchise.api.mapper.FranchiseMapper;
import com.franchise.api.repository.FranchiseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FranchiseService {

    private final FranchiseRepository franchiseRepository;

    public FranchiseResponse createFranchise(CreateFranchiseRequest request) {
        String name = normalizeName(request.name());
        ensureNameIsPresent(name, "Franchise name is required");
        franchiseRepository.findByNameIgnoreCase(name)
                .ifPresent(existing -> {
                    throw new ConflictException("Franchise with name '%s' already exists".formatted(name));
                });
        Franchise franchise = Franchise.builder()
                .name(name)
                .active(request.active() == null || Boolean.TRUE.equals(request.active()))
                .build();
        Franchise saved = franchiseRepository.save(franchise);
        return FranchiseMapper.toResponse(saved);
    }

    public FranchiseResponse updateFranchiseName(String franchiseId, UpdateFranchiseNameRequest request) {
        Franchise franchise = getFranchise(franchiseId);
        String newName = normalizeName(request.name());
        ensureNameIsPresent(newName, "Franchise name is required");
        boolean sameName = franchise.getName() != null && franchise.getName().equalsIgnoreCase(newName);
        if (!sameName) {
            franchiseRepository.findByNameIgnoreCase(newName)
                    .filter(existing -> !existing.getId().equals(franchiseId))
                    .ifPresent(existing -> {
                        throw new ConflictException("Franchise with name '%s' already exists".formatted(newName));
                    });
            franchise.setName(newName);
            franchise = franchiseRepository.save(franchise);
        }
        return FranchiseMapper.toResponse(franchise);
    }

    public FranchiseResponse getFranchiseById(String franchiseId) {
        return FranchiseMapper.toResponse(getFranchise(franchiseId));
    }

    public List<FranchiseResponse> getAllFranchises() {
        return franchiseRepository.findAll().stream()
                .map(FranchiseMapper::toResponse)
                .toList();
    }

    public FranchiseResponse updateFranchiseStatus(String franchiseId, UpdateFranchiseStatusRequest request) {
        Franchise franchise = getFranchise(franchiseId);
        boolean currentActive = isActive(franchise);
        boolean requested = Boolean.TRUE.equals(request.active());
        if (currentActive != requested) {
            franchise.setActive(requested);
            franchiseRepository.save(franchise);
        }
        return FranchiseMapper.toResponse(franchise);
    }

    public BranchResponse addBranch(String franchiseId, CreateBranchRequest request) {
        Franchise franchise = getFranchise(franchiseId);
        if (!isActive(franchise)) {
            throw new BadRequestException("Cannot add branches to an inactive franchise");
        }
        String name = normalizeName(request.name());
        ensureNameIsPresent(name, "Branch name is required");
        List<Branch> branches = ensureBranches(franchise);
        boolean exists = branches.stream().anyMatch(branch -> branch.getName() != null && branch.getName().equalsIgnoreCase(name));
        if (exists) {
            throw new ConflictException("Branch with name '%s' already exists in franchise".formatted(name));
        }
        boolean active = request.active() == null || Boolean.TRUE.equals(request.active());
        Branch branch = Branch.builder()
                .id(UUID.randomUUID().toString())
                .name(name)
                .active(active)
                .build();
        branches.add(branch);
        franchiseRepository.save(franchise);
        return FranchiseMapper.toBranchResponse(branch);
    }

    public BranchResponse updateBranchName(String franchiseId, String branchId, UpdateBranchNameRequest request) {
        Franchise franchise = getFranchise(franchiseId);
        Branch branch = getBranch(franchise, branchId);
        String newName = normalizeName(request.name());
        ensureNameIsPresent(newName, "Branch name is required");
        boolean sameName = branch.getName() != null && branch.getName().equalsIgnoreCase(newName);
        if (!sameName) {
            boolean duplicate = ensureBranches(franchise).stream()
                    .anyMatch(other -> !other.getId().equals(branchId) && other.getName() != null && other.getName().equalsIgnoreCase(newName));
            if (duplicate) {
                throw new ConflictException("Branch with name '%s' already exists in franchise".formatted(newName));
            }
            branch.setName(newName);
            franchiseRepository.save(franchise);
        }
        return FranchiseMapper.toBranchResponse(branch);
    }

    public BranchResponse updateBranchStatus(String franchiseId, String branchId, UpdateBranchStatusRequest request) {
        Franchise franchise = getFranchise(franchiseId);
        Branch branch = getBranch(franchise, branchId);
        boolean requestedStatus = Boolean.TRUE.equals(request.active());
        if (branch.isActive() != requestedStatus) {
            branch.setActive(requestedStatus);
            franchiseRepository.save(franchise);
        }
        return FranchiseMapper.toBranchResponse(branch);
    }

    public ProductResponse addProduct(String franchiseId, String branchId, CreateProductRequest request) {
        Franchise franchise = getFranchise(franchiseId);
        if (!isActive(franchise)) {
            throw new BadRequestException("Cannot add products to an inactive franchise");
        }
        Branch branch = getBranch(franchise, branchId);
        if (!branch.isActive()) {
            throw new BadRequestException("Cannot add products to an inactive branch");
        }
        String name = normalizeName(request.name());
        ensureNameIsPresent(name, "Product name is required");
        int stock = ensureNonNegativeStock(request.stock());
        List<Product> products = ensureProducts(branch);
        boolean exists = products.stream().anyMatch(product -> product.getName() != null && product.getName().equalsIgnoreCase(name));
        if (exists) {
            throw new ConflictException("Product with name '%s' already exists in branch".formatted(name));
        }
        Product product = Product.builder()
                .id(UUID.randomUUID().toString())
                .name(name)
                .stock(stock)
                .build();
        products.add(product);
        franchiseRepository.save(franchise);
        return FranchiseMapper.toProductResponse(product);
    }

    public void deleteProduct(String franchiseId, String branchId, String productId) {
        Franchise franchise = getFranchise(franchiseId);
        Branch branch = getBranch(franchise, branchId);
        List<Product> products = ensureProducts(branch);
        boolean removed = products.removeIf(product -> product.getId().equals(productId));
        if (!removed) {
            throw new ResourceNotFoundException("Product with id '%s' not found".formatted(productId));
        }
        franchiseRepository.save(franchise);
    }

    public ProductResponse updateProductStock(String franchiseId, String branchId, String productId, UpdateProductStockRequest request) {
        Franchise franchise = getFranchise(franchiseId);
        Branch branch = getBranch(franchise, branchId);
        Product product = getProduct(branch, productId);
        int stock = ensureNonNegativeStock(request.stock());
        product.setStock(stock);
        franchiseRepository.save(franchise);
        return FranchiseMapper.toProductResponse(product);
    }

    public ProductResponse updateProductName(String franchiseId, String branchId, String productId, UpdateProductNameRequest request) {
        Franchise franchise = getFranchise(franchiseId);
        Branch branch = getBranch(franchise, branchId);
        Product product = getProduct(branch, productId);
        String newName = normalizeName(request.name());
        ensureNameIsPresent(newName, "Product name is required");
        boolean sameName = product.getName() != null && product.getName().equalsIgnoreCase(newName);
        if (!sameName) {
            boolean duplicate = ensureProducts(branch).stream()
                    .anyMatch(other -> !other.getId().equals(productId) && other.getName() != null && other.getName().equalsIgnoreCase(newName));
            if (duplicate) {
                throw new ConflictException("Product with name '%s' already exists in branch".formatted(newName));
            }
            product.setName(newName);
            franchiseRepository.save(franchise);
        }
        return FranchiseMapper.toProductResponse(product);
    }

    public void deleteBranch(String franchiseId, String branchId) {
        Franchise franchise = getFranchise(franchiseId);
        List<Branch> branches = ensureBranches(franchise);
        boolean removed = branches.removeIf(branch -> branchId.equals(branch.getId()));
        if (!removed) {
            throw new ResourceNotFoundException("Branch with id '%s' not found in franchise".formatted(branchId));
        }
        franchiseRepository.save(franchise);
    }

    public void deleteFranchise(String franchiseId) {
        Franchise franchise = getFranchise(franchiseId);
        franchiseRepository.delete(franchise);
    }

    public List<TopProductPerBranchResponse> getTopProductPerBranch(String franchiseId) {
        Franchise franchise = getFranchise(franchiseId);
        if (!isActive(franchise)) {
            return List.of();
        }
        return ensureBranches(franchise).stream()
                .filter(Branch::isActive)
                .map(branch -> {
                    Optional<Product> topProduct = ensureProducts(branch).stream()
                            .max(Comparator.comparingInt(Product::getStock));
                    return topProduct.map(product -> new TopProductPerBranchResponse(
                            branch.getId(),
                            branch.getName(),
                            FranchiseMapper.toProductResponse(product)
                    ));
                })
                .flatMap(Optional::stream)
                .toList();
    }

    private Franchise getFranchise(String franchiseId) {
        return franchiseRepository.findById(franchiseId)
                .orElseThrow(() -> new ResourceNotFoundException("Franchise with id '%s' not found".formatted(franchiseId)));
    }

    private Branch getBranch(Franchise franchise, String branchId) {
        return ensureBranches(franchise).stream()
                .filter(branch -> branch.getId().equals(branchId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Branch with id '%s' not found".formatted(branchId)));
    }

    private Product getProduct(Branch branch, String productId) {
        return ensureProducts(branch).stream()
                .filter(product -> product.getId().equals(productId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Product with id '%s' not found".formatted(productId)));
    }

    private List<Branch> ensureBranches(Franchise franchise) {
        if (franchise.getBranches() == null) {
            franchise.setBranches(new ArrayList<>());
        }
        return franchise.getBranches();
    }

    private List<Product> ensureProducts(Branch branch) {
        if (branch.getProducts() == null) {
            branch.setProducts(new ArrayList<>());
        }
        return branch.getProducts();
    }

    private boolean isActive(Franchise franchise) {
        return franchise.getActive() == null || Boolean.TRUE.equals(franchise.getActive());
    }

    private String normalizeName(String name) {
        if (name == null) {
            return null;
        }
        return StringUtils.trimWhitespace(name);
    }

    private void ensureNameIsPresent(String name, String message) {
        if (!StringUtils.hasText(name)) {
            throw new BadRequestException(message);
        }
    }

    private int ensureNonNegativeStock(Integer stock) {
        if (stock == null) {
            throw new BadRequestException("Stock value is required");
        }
        if (stock < 0) {
            throw new BadRequestException("Stock must be greater than or equal to 0");
        }
        return stock;
    }
}
