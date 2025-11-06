package com.franchise.api.security;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
@RequiredArgsConstructor
public class SecurityDataInitializer implements CommandLineRunner {

    private final UserAccountRepository repository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        repository.findByUsernameIgnoreCase("admin")
                .orElseGet(() -> repository.save(UserAccount.builder()
                        .username("admin")
                        .password(passwordEncoder.encode("Admin123!"))
                        .fullName("Administrador Global")
                        .email("cfca5@hotmail.com")
                        .roles(Set.of(Role.ADMIN, Role.USER))
                        .build()));

        repository.findByUsernameIgnoreCase("analyst")
                .orElseGet(() -> repository.save(UserAccount.builder()
                        .username("analyst")
                        .password(passwordEncoder.encode("Analyst123!"))
                        .fullName("Analista Invitado")
                        .email("analyst@example.com")
                        .roles(Set.of(Role.USER))
                        .build()));
    }
}
