package com.franchise.api.service.unit;

import com.franchise.api.domain.Branch;
import com.franchise.api.domain.Franchise;
import com.franchise.api.domain.Product;
import com.franchise.api.dto.BranchResponse;
import com.franchise.api.dto.CreateBranchRequest;
import com.franchise.api.dto.CreateProductRequest;
import com.franchise.api.dto.CreateFranchiseRequest;
import com.franchise.api.dto.TopProductPerBranchResponse;
import com.franchise.api.dto.UpdateBranchStatusRequest;
import com.franchise.api.dto.UpdateFranchiseStatusRequest;
import com.franchise.api.exception.BadRequestException;
import com.franchise.api.exception.ConflictException;
import com.franchise.api.exception.ResourceNotFoundException;
import com.franchise.api.repository.FranchiseRepository;
import com.franchise.api.service.FranchiseService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FranchiseServiceUnitTest {

    @Mock
    private FranchiseRepository franchiseRepository;

    @InjectMocks
    private FranchiseService franchiseService;

    @BeforeEach
    void setUp() {
        franchiseService = new FranchiseService(franchiseRepository);
    }

    @Test
    void createFranchiseShouldPersistNewEntity() {
        CreateFranchiseRequest request = new CreateFranchiseRequest("New Franchise", null);
        when(franchiseRepository.findByNameIgnoreCase("New Franchise")).thenReturn(Optional.empty());
        when(franchiseRepository.save(any(Franchise.class))).thenAnswer(invocation -> {
            Franchise franchise = invocation.getArgument(0);
            franchise.setId("generated-id");
            return franchise;
        });

        var response = franchiseService.createFranchise(request);

        assertThat(response.id()).isEqualTo("generated-id");
        assertThat(response.name()).isEqualTo("New Franchise");
        assertThat(response.branches()).isEmpty();
        assertThat(response.active()).isTrue();
    }

    @Test
    void createFranchiseShouldFailWhenNameAlreadyExists() {
        CreateFranchiseRequest request = new CreateFranchiseRequest("Existing", null);
        when(franchiseRepository.findByNameIgnoreCase("Existing")).thenReturn(Optional.of(new Franchise()));

        assertThrows(ConflictException.class, () -> franchiseService.createFranchise(request));
    }

    @Test
    void addBranchShouldAppendBranchToFranchise() {
        Franchise franchise = Franchise.builder()
                .id("franchise-1")
                .name("Franchise")
                .active(true)
                .branches(new ArrayList<>())
                .build();
        when(franchiseRepository.findById("franchise-1")).thenReturn(Optional.of(franchise));
        when(franchiseRepository.save(franchise)).thenReturn(franchise);

        BranchResponse response = franchiseService.addBranch("franchise-1", new CreateBranchRequest("Centro", null));

        assertThat(response.name()).isEqualTo("Centro");
        assertThat(response.products()).isEmpty();
        assertThat(response.id()).isNotBlank();
        assertThat(response.active()).isTrue();

        ArgumentCaptor<Franchise> captor = ArgumentCaptor.forClass(Franchise.class);
        verify(franchiseRepository).save(captor.capture());
        assertThat(captor.getValue().getBranches()).hasSize(1);
    }

    @Test
    void addBranchShouldRespectActiveFlag() {
        Franchise franchise = Franchise.builder()
                .id("franchise-1")
                .name("Franchise")
                .active(true)
                .branches(new ArrayList<>())
                .build();
        when(franchiseRepository.findById("franchise-1")).thenReturn(Optional.of(franchise));
        when(franchiseRepository.save(franchise)).thenReturn(franchise);

        BranchResponse response = franchiseService.addBranch("franchise-1", new CreateBranchRequest("Centro", false));

        assertThat(response.active()).isFalse();
    }

    @Test
    void addBranchShouldThrowWhenFranchiseMissing() {
        when(franchiseRepository.findById("missing")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> franchiseService.addBranch("missing", new CreateBranchRequest("Centro", null)));
    }

    @Test
    void addBranchShouldFailWhenFranchiseInactive() {
        Franchise franchise = Franchise.builder()
                .id("franchise-1")
                .name("Franchise")
                .active(false)
                .branches(new ArrayList<>())
                .build();
        when(franchiseRepository.findById("franchise-1")).thenReturn(Optional.of(franchise));

        assertThrows(BadRequestException.class,
                () -> franchiseService.addBranch("franchise-1", new CreateBranchRequest("Centro", null)));
    }

    @Test
    void getTopProductPerBranchShouldReturnProductWithHighestStock() {
        Franchise franchise = Franchise.builder()
                .id("franchise-1")
                .name("Franchise")
                .active(true)
                .branches(new ArrayList<>())
                .build();
        Branch branch = Branch.builder()
                .id("branch-1")
                .name("Centro")
                .products(new ArrayList<>())
                .build();
        branch.getProducts().add(Product.builder().id("p1").name("Burger").stock(10).build());
        branch.getProducts().add(Product.builder().id("p2").name("Pizza").stock(25).build());
        franchise.getBranches().add(branch);

        when(franchiseRepository.findById("franchise-1")).thenReturn(Optional.of(franchise));

        List<TopProductPerBranchResponse> topProducts = franchiseService.getTopProductPerBranch("franchise-1");

        assertThat(topProducts).hasSize(1);
        TopProductPerBranchResponse response = topProducts.get(0);
        assertThat(response.branchId()).isEqualTo("branch-1");
        assertThat(response.product().id()).isEqualTo("p2");
        assertThat(response.product().stock()).isEqualTo(25);
    }

    @Test
    void updateFranchiseStatusShouldToggleActiveFlag() {
        Franchise franchise = Franchise.builder()
                .id("franchise-1")
                .name("Franchise")
                .active(true)
                .build();
        when(franchiseRepository.findById("franchise-1")).thenReturn(Optional.of(franchise));
        when(franchiseRepository.save(franchise)).thenReturn(franchise);

        var response = franchiseService.updateFranchiseStatus("franchise-1", new UpdateFranchiseStatusRequest(false));

        assertThat(response.active()).isFalse();
        verify(franchiseRepository).save(franchise);
    }

    @Test
    void updateBranchStatusShouldToggleActive() {
        Branch branch = Branch.builder()
                .id("branch-1")
                .name("Centro")
                .active(true)
                .products(new ArrayList<>())
                .build();
        Franchise franchise = Franchise.builder()
                .id("franchise-1")
                .name("Franchise")
                .active(true)
                .branches(new ArrayList<>(List.of(branch)))
                .build();
        when(franchiseRepository.findById("franchise-1")).thenReturn(Optional.of(franchise));
        when(franchiseRepository.save(franchise)).thenReturn(franchise);

        BranchResponse response = franchiseService.updateBranchStatus("franchise-1", "branch-1", new UpdateBranchStatusRequest(false));

        assertThat(response.active()).isFalse();
        verify(franchiseRepository).save(franchise);
    }

    @Test
    void deleteBranchShouldRemoveBranch() {
        Branch branch = Branch.builder()
                .id("branch-1")
                .name("Centro")
                .products(new ArrayList<>())
                .build();
        Franchise franchise = Franchise.builder()
                .id("franchise-1")
                .name("Franchise")
                .active(true)
                .branches(new ArrayList<>(List.of(branch)))
                .build();
        when(franchiseRepository.findById("franchise-1")).thenReturn(Optional.of(franchise));
        when(franchiseRepository.save(franchise)).thenReturn(franchise);

        franchiseService.deleteBranch("franchise-1", "branch-1");

        assertThat(franchise.getBranches()).isEmpty();
        verify(franchiseRepository).save(franchise);
    }

    @Test
    void deleteFranchiseShouldDelegateToRepository() {
        Franchise franchise = Franchise.builder()
                .id("franchise-1")
                .name("Franchise")
                .active(true)
                .build();
        when(franchiseRepository.findById("franchise-1")).thenReturn(Optional.of(franchise));

        franchiseService.deleteFranchise("franchise-1");

        verify(franchiseRepository).delete(franchise);
    }

    @Test
    void addProductShouldFailWhenBranchInactive() {
        Branch branch = Branch.builder()
                .id("branch-1")
                .name("Inactive")
                .active(false)
                .products(new ArrayList<>())
                .build();
        Franchise franchise = Franchise.builder()
                .id("franchise-1")
                .name("Franchise")
                .active(true)
                .branches(new ArrayList<>(List.of(branch)))
                .build();
        when(franchiseRepository.findById("franchise-1")).thenReturn(Optional.of(franchise));

        assertThrows(BadRequestException.class, () ->
                franchiseService.addProduct("franchise-1", "branch-1", new CreateProductRequest("Burger", 10)));
    }

    @Test
    void addProductShouldFailWhenFranchiseInactive() {
        Branch branch = Branch.builder()
                .id("branch-1")
                .name("Active Branch")
                .active(true)
                .products(new ArrayList<>())
                .build();
        Franchise franchise = Franchise.builder()
                .id("franchise-1")
                .name("Franchise")
                .active(false)
                .branches(new ArrayList<>(List.of(branch)))
                .build();
        when(franchiseRepository.findById("franchise-1")).thenReturn(Optional.of(franchise));

        assertThrows(BadRequestException.class, () ->
                franchiseService.addProduct("franchise-1", "branch-1", new CreateProductRequest("Burger", 10)));
    }

    @Test
    void getTopProductPerBranchShouldIgnoreInactiveBranches() {
        Branch inactiveBranch = Branch.builder()
                .id("branch-inactive")
                .name("Inactive")
                .active(false)
                .products(new ArrayList<>(List.of(Product.builder().id("p1").name("Burger").stock(10).build())))
                .build();
        Franchise franchise = Franchise.builder()
                .id("franchise-1")
                .name("Franchise")
                .active(true)
                .branches(new ArrayList<>(List.of(inactiveBranch)))
                .build();
        when(franchiseRepository.findById("franchise-1")).thenReturn(Optional.of(franchise));

        List<TopProductPerBranchResponse> topProducts = franchiseService.getTopProductPerBranch("franchise-1");

        assertThat(topProducts).isEmpty();
    }
}
