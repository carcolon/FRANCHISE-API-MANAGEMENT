package com.franchise.api.controller.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.franchise.api.domain.Franchise;
import com.franchise.api.repository.FranchiseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class FranchiseControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private FranchiseRepository franchiseRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @DynamicPropertySource
    static void mongoProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.database", () -> "franchise_it_" + System.nanoTime());
        registry.add("spring.mongodb.embedded.version", () -> "6.0.5");
    }

    @BeforeEach
    void cleanDatabase() {
        franchiseRepository.deleteAll();
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void shouldCreateFranchiseViaRestEndpoint() throws Exception {
        mockMvc.perform(post("/api/v1/franchises")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Integration Franchise",
                                  "active": true
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Integration Franchise"))
                .andExpect(jsonPath("$.active").value(true));

        assertThat(franchiseRepository.findAll()).hasSize(1);
    }

    @Test
    @WithMockUser(username = "viewer", roles = "USER")
    void shouldAllowReadAccessToUserRole() throws Exception {
        franchiseRepository.save(Franchise.builder()
                .name("Existing Franchise")
                .active(true)
                .build());

        mockMvc.perform(get("/api/v1/franchises"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Existing Franchise"));
    }

    @Test
    @WithMockUser(username = "viewer", roles = "USER")
    void shouldBlockWriteOperationsForNonAdmins() throws Exception {
        mockMvc.perform(post("/api/v1/franchises")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateFranchisePayload("Forbidden Franchise"))))
                .andExpect(status().isForbidden());
    }

    private record CreateFranchisePayload(String name) {}
}
