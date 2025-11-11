package com.franchise.api.controller.integration;

import com.franchise.api.security.Role;
import com.franchise.api.security.UserAccount;
import com.franchise.api.security.UserAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @DynamicPropertySource
    static void mongoProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.database", () -> "auth_it_" + System.nanoTime());
        registry.add("spring.mongodb.embedded.version", () -> "6.0.5");
    }

    @BeforeEach
    void setUp() {
        userAccountRepository.deleteAll();
        userAccountRepository.save(UserAccount.builder()
                .username("integration-admin")
                .password(passwordEncoder.encode("Admin123!"))
                .roles(Set.of(Role.ADMIN))
                .passwordChangeRequired(true)
                .active(true)
                .build());
    }

    @Test
    void loginShouldReturnPasswordChangeFlag() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "integration-admin",
                                  "password": "Admin123!"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("integration-admin"))
                .andExpect(jsonPath("$.passwordChangeRequired").value(true));
    }

    @Test
    @WithMockUser(username = "integration-admin")
    void changePasswordEndpointShouldResetFlag() throws Exception {
        mockMvc.perform(post("/api/v1/auth/change-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "currentPassword": "Admin123!",
                                  "newPassword": "NewPass123!"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Contrasena actualizada correctamente."));

        UserAccount updated = userAccountRepository.findByUsernameIgnoreCase("integration-admin").orElseThrow();
        assertThat(updated.isPasswordChangeRequired()).isFalse();
        assertThat(passwordEncoder.matches("NewPass123!", updated.getPassword())).isTrue();
    }
}
