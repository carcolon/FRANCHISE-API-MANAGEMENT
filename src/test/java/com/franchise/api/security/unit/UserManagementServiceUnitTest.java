package com.franchise.api.security.unit;

import com.franchise.api.dto.CreateUserRequest;
import com.franchise.api.dto.UpdateUserStatusRequest;
import com.franchise.api.dto.UserResponse;
import com.franchise.api.exception.BadRequestException;
import com.franchise.api.exception.ConflictException;
import com.franchise.api.exception.ResourceNotFoundException;
import com.franchise.api.security.Role;
import com.franchise.api.security.UserAccount;
import com.franchise.api.security.UserAccountRepository;
import com.franchise.api.security.UserManagementService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserManagementServiceUnitTest {

    @Mock
    private UserAccountRepository repository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserManagementService service;

    @Test
    void createUserShouldPersistAndReturnResponse() {
        CreateUserRequest request = new CreateUserRequest(
                "newUser",
                "SecurePass123!",
                "Nuevo Usuario",
                "new.user@example.com",
                Set.of("USER")
        );
        when(repository.findByUsernameIgnoreCase("newUser")).thenReturn(Optional.empty());
        when(repository.findByEmailIgnoreCase("new.user@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("SecurePass123!")).thenReturn("encoded");
        when(repository.save(any(UserAccount.class))).thenAnswer(invocation -> {
            UserAccount account = invocation.getArgument(0);
            account.setId("generated-id");
            return account;
        });

        UserResponse response = service.createUser(request);

        assertThat(response.id()).isEqualTo("generated-id");
        assertThat(response.username()).isEqualTo("newUser");
        assertThat(response.email()).isEqualTo("new.user@example.com");
        assertThat(response.roles()).containsExactly("USER");

        ArgumentCaptor<UserAccount> captor = ArgumentCaptor.forClass(UserAccount.class);
        verify(repository).save(captor.capture());
        UserAccount saved = captor.getValue();
        assertThat(saved.getFullName()).isEqualTo("Nuevo Usuario");
        assertThat(saved.getPassword()).isEqualTo("encoded");
        assertThat(saved.isPasswordChangeRequired()).isTrue();
    }

    @Test
    void createUserShouldFailWhenUsernameExists() {
        CreateUserRequest request = new CreateUserRequest(
                "admin",
                "SecurePass123!",
                "Nuevo Usuario",
                "duplicate@example.com",
                Set.of("USER")
        );
        when(repository.findByUsernameIgnoreCase("admin")).thenReturn(Optional.of(new UserAccount()));

        assertThrows(ConflictException.class, () -> service.createUser(request));
    }

    @Test
    void createUserShouldFailWhenEmailExists() {
        CreateUserRequest request = new CreateUserRequest(
                "uniqueUser",
                "SecurePass123!",
                "Nuevo Usuario",
                "duplicate@example.com",
                Set.of("USER")
        );
        when(repository.findByUsernameIgnoreCase("uniqueUser")).thenReturn(Optional.empty());
        when(repository.findByEmailIgnoreCase("duplicate@example.com")).thenReturn(Optional.of(new UserAccount()));

        assertThrows(ConflictException.class, () -> service.createUser(request));
    }

    @Test
    void createUserShouldFailWithInvalidRole() {
        CreateUserRequest request = new CreateUserRequest(
                "uniqueUser",
                "SecurePass123!",
                "Nuevo Usuario",
                "valid@example.com",
                Set.of("MANAGER")
        );
        when(repository.findByUsernameIgnoreCase("uniqueUser")).thenReturn(Optional.empty());
        when(repository.findByEmailIgnoreCase("valid@example.com")).thenReturn(Optional.empty());

        assertThrows(BadRequestException.class, () -> service.createUser(request));
    }

    @Test
    void getUsersShouldMapResponse() {
        UserAccount account = UserAccount.builder()
                .id("user-1")
                .username("admin")
                .fullName("Admin User")
                .email("admin@example.com")
                .roles(Set.of(Role.ADMIN, Role.USER))
                .active(true)
                .build();
        when(repository.findAll()).thenReturn(List.of(account));

        List<UserResponse> result = service.getUsers();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).roles()).contains("ADMIN", "USER");
    }

    @Test
    void updateStatusShouldPersistChanges() {
        UserAccount account = UserAccount.builder()
                .id("user-1")
                .username("admin")
                .active(true)
                .build();
        when(repository.findById("user-1")).thenReturn(Optional.of(account));
        when(repository.save(any(UserAccount.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserResponse response = service.updateStatus("user-1", new UpdateUserStatusRequest(false));

        assertThat(response.active()).isFalse();
    }

    @Test
    void updateStatusShouldFailWhenUserMissing() {
        when(repository.findById("missing")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.updateStatus("missing", new UpdateUserStatusRequest(true)));
    }

    @Test
    void updateStatusShouldFailWhenStatusNull() {
        assertThrows(BadRequestException.class,
                () -> service.updateStatus("id-1", new UpdateUserStatusRequest(null)));
    }

    @Test
    void deleteUserShouldRemoveAccount() {
        UserAccount account = UserAccount.builder()
                .id("user-1")
                .username("admin")
                .build();
        when(repository.findById("user-1")).thenReturn(Optional.of(account));

        service.deleteUser("user-1");

        verify(repository).delete(account);
    }

    @Test
    void deleteUserShouldFailWhenNotFound() {
        when(repository.findById("missing")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.deleteUser("missing"));
    }
}
