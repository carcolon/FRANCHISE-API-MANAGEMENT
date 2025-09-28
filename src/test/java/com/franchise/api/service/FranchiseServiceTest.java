package com.franchise.api.service;

import com.franchise.api.domain.Branch;
import com.franchise.api.domain.Franchise;
import com.franchise.api.domain.Product;
import com.franchise.api.dto.BranchResponse;
import com.franchise.api.dto.CreateBranchRequest;
import com.franchise.api.dto.CreateFranchiseRequest;
import com.franchise.api.dto.TopProductPerBranchResponse;
import com.franchise.api.exception.ConflictException;
import com.franchise.api.exception.ResourceNotFoundException;
import com.franchise.api.repository.FranchiseRepository;
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
class FranchiseServiceTest {

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
        CreateFranchiseRequest request = new CreateFranchiseRequest("New Franchise");
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
    }

    @Test
    void createFranchiseShouldFailWhenNameAlreadyExists() {
        CreateFranchiseRequest request = new CreateFranchiseRequest("Existing");
        when(franchiseRepository.findByNameIgnoreCase("Existing")).thenReturn(Optional.of(new Franchise()));

        assertThrows(ConflictException.class, () -> franchiseService.createFranchise(request));
    }

    @Test
    void addBranchShouldAppendBranchToFranchise() {
        Franchise franchise = Franchise.builder()
                .id("franchise-1")
                .name("Franchise")
                .branches(new ArrayList<>())
                .build();
        when(franchiseRepository.findById("franchise-1")).thenReturn(Optional.of(franchise));
        when(franchiseRepository.save(franchise)).thenReturn(franchise);

        BranchResponse response = franchiseService.addBranch("franchise-1", new CreateBranchRequest("Centro"));

        assertThat(response.name()).isEqualTo("Centro");
        assertThat(response.products()).isEmpty();
        assertThat(response.id()).isNotBlank();

        ArgumentCaptor<Franchise> captor = ArgumentCaptor.forClass(Franchise.class);
        verify(franchiseRepository).save(captor.capture());
        assertThat(captor.getValue().getBranches()).hasSize(1);
    }

    @Test
    void addBranchShouldThrowWhenFranchiseMissing() {
        when(franchiseRepository.findById("missing")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> franchiseService.addBranch("missing", new CreateBranchRequest("Centro")));
    }

    @Test
    void getTopProductPerBranchShouldReturnProductWithHighestStock() {
        Franchise franchise = Franchise.builder()
                .id("franchise-1")
                .name("Franchise")
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
}
