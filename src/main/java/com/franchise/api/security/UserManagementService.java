package com.franchise.api.security;

import com.franchise.api.dto.CreateUserRequest;
import com.franchise.api.dto.UpdateUserStatusRequest;
import com.franchise.api.dto.UserResponse;
import com.franchise.api.exception.BadRequestException;
import com.franchise.api.exception.ConflictException;
import com.franchise.api.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class UserManagementService {

    private final UserAccountRepository repository;
    private final PasswordEncoder passwordEncoder;

    public UserResponse createUser(CreateUserRequest request) {
        String username = normalize(request.username(), "El nombre de usuario es obligatorio");
        String fullName = normalize(request.fullName(), "El nombre completo es obligatorio");
        String email = normalizeEmail(request.email());

        repository.findByUsernameIgnoreCase(username)
                .ifPresent(existing -> {
                    throw new ConflictException("Ya existe un usuario con el nombre '%s'".formatted(username));
                });
        repository.findByEmailIgnoreCase(email)
                .ifPresent(existing -> {
                    throw new ConflictException("Ya existe un usuario con el correo '%s'".formatted(email));
                });

        Set<Role> roles = resolveRoles(request.roles());
        String encodedPassword = passwordEncoder.encode(request.password());

        UserAccount account = UserAccount.builder()
                .username(username)
                .password(encodedPassword)
                .fullName(fullName)
                .email(email)
                .roles(roles)
                .active(true)
                .passwordChangeRequired(true)
                .build();
        UserAccount saved = repository.save(account);
        return toResponse(saved);
    }

    public List<UserResponse> getUsers() {
        return repository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    public UserResponse updateStatus(String userId, UpdateUserStatusRequest request) {
        if (request.active() == null) {
            throw new BadRequestException("El estado 'active' es obligatorio");
        }
        UserAccount account = repository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario con id '%s' no encontrado".formatted(userId)));
        account.setActive(request.active());
        UserAccount saved = repository.save(account);
        return toResponse(saved);
    }

    public void deleteUser(String userId) {
        UserAccount account = repository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario con id '%s' no encontrado".formatted(userId)));
        repository.delete(account);
    }

    private Set<Role> resolveRoles(Set<String> requestRoles) {
        if (requestRoles == null || requestRoles.isEmpty()) {
            return EnumSet.of(Role.USER);
        }
        EnumSet<Role> roles = EnumSet.noneOf(Role.class);
        for (String roleValue : requestRoles) {
            if (!StringUtils.hasText(roleValue)) {
                continue;
            }
            try {
                roles.add(Role.valueOf(roleValue.trim().toUpperCase(Locale.ROOT)));
            } catch (IllegalArgumentException ex) {
                throw new BadRequestException("Rol '%s' no es valido. Valores permitidos: ADMIN, USER".formatted(roleValue));
            }
        }
        if (roles.isEmpty()) {
            return EnumSet.of(Role.USER);
        }
        return roles;
    }

    private UserResponse toResponse(UserAccount account) {
        return new UserResponse(
                account.getId(),
                account.getUsername(),
                account.getFullName(),
                account.getEmail(),
                account.isActive(),
                account.getRoles().stream().map(Role::name).toList()
        );
    }

    private String normalize(String value, String message) {
        if (!StringUtils.hasText(value)) {
            throw new BadRequestException(message);
        }
        return value.trim();
    }

    private String normalizeEmail(String email) {
        String normalized = normalize(email, "El correo es obligatorio");
        return normalized.toLowerCase(Locale.ROOT);
    }
}
