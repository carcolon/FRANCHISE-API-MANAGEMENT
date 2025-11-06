package com.franchise.api.security;

import com.franchise.api.dto.ForgotPasswordResponse;
import com.franchise.api.dto.MessageResponse;
import com.franchise.api.exception.BadRequestException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {

    @Mock
    private UserAccountRepository repository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private PasswordResetService passwordResetService;

    @Test
    void initiateResetShouldGenerateTokenWhenUserExists() {
        UserAccount account = UserAccount.builder()
                .id("user-1")
                .username("admin")
                .password("encoded")
                .roles(Set.of(Role.ADMIN))
                .active(true)
                .build();
        when(repository.findByUsernameIgnoreCase("admin")).thenReturn(Optional.of(account));
        when(repository.save(any(UserAccount.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ForgotPasswordResponse response = passwordResetService.initiateReset("admin");

        assertThat(response.message()).contains("recibiras");
        assertThat(response.resetToken()).isNotBlank();
        ArgumentCaptor<UserAccount> captor = ArgumentCaptor.forClass(UserAccount.class);
        verify(repository).save(captor.capture());
        UserAccount saved = captor.getValue();
        assertThat(saved.getPasswordResetToken()).isNotBlank();
        assertThat(saved.getPasswordResetExpiration()).isAfter(Instant.now());
    }

    @Test
    void initiateResetShouldReturnGenericMessageWhenUserNotFound() {
        when(repository.findByUsernameIgnoreCase("missing")).thenReturn(Optional.empty());

        ForgotPasswordResponse response = passwordResetService.initiateReset("missing");

        assertThat(response.message()).contains("recibiras instrucciones");
        assertThat(response.resetToken()).isNull();
    }

    @Test
    void resetPasswordShouldUpdatePasswordWhenTokenValid() {
        UserAccount account = UserAccount.builder()
                .id("user-1")
                .username("admin")
                .password("old")
                .passwordResetToken("token-123")
                .passwordResetExpiration(Instant.now().plusSeconds(300))
                .passwordChangeRequired(true)
                .build();
        when(repository.findByPasswordResetToken("token-123")).thenReturn(Optional.of(account));
        when(passwordEncoder.encode("NewPass123!")).thenReturn("encoded-new");
        when(repository.save(any(UserAccount.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MessageResponse response = passwordResetService.resetPassword("token-123", "NewPass123!");

        assertThat(response.message()).contains("Contrasena actualizada");
        assertThat(account.getPassword()).isEqualTo("encoded-new");
        assertThat(account.getPasswordResetToken()).isNull();
        assertThat(account.getPasswordResetExpiration()).isNull();
        assertThat(account.isPasswordChangeRequired()).isFalse();
    }

    @Test
    void resetPasswordShouldFailWhenTokenExpired() {
        UserAccount account = UserAccount.builder()
                .id("user-1")
                .username("admin")
                .password("old")
                .passwordResetToken("token-123")
                .passwordResetExpiration(Instant.now().minusSeconds(60))
                .build();
        when(repository.findByPasswordResetToken("token-123")).thenReturn(Optional.of(account));

        assertThrows(BadRequestException.class,
                () -> passwordResetService.resetPassword("token-123", "NewPass123!"));
    }

    @Test
    void resetPasswordShouldFailWhenTokenInvalid() {
        when(repository.findByPasswordResetToken("invalid")).thenReturn(Optional.empty());

        assertThrows(BadRequestException.class,
                () -> passwordResetService.resetPassword("invalid", "NewPass123!"));
    }

    @Test
    void validateTokenShouldReturnSuccessWhenValid() {
        UserAccount account = UserAccount.builder()
                .id("user-1")
                .username("admin")
                .passwordResetToken("token-321")
                .passwordResetExpiration(Instant.now().plusSeconds(120))
                .build();
        when(repository.findByPasswordResetToken("token-321")).thenReturn(Optional.of(account));

        MessageResponse response = passwordResetService.validateToken("token-321");

        assertThat(response.message()).contains("Token valido");
    }

    @Test
    void validateTokenShouldFailWhenExpired() {
        UserAccount account = UserAccount.builder()
                .id("user-1")
                .passwordResetToken("token-321")
                .passwordResetExpiration(Instant.now().minusSeconds(5))
                .build();
        when(repository.findByPasswordResetToken("token-321")).thenReturn(Optional.of(account));

        assertThrows(BadRequestException.class,
                () -> passwordResetService.validateToken("token-321"));
    }

    @Test
    void changePasswordShouldUpdateWhenCurrentMatches() {
        UserAccount account = UserAccount.builder()
                .id("user-1")
                .username("admin")
                .password("encoded-old")
                .passwordChangeRequired(true)
                .build();
        when(repository.findByUsernameIgnoreCase("admin")).thenReturn(Optional.of(account));
        when(passwordEncoder.matches("OldPass123!", "encoded-old")).thenReturn(true);
        when(passwordEncoder.encode("NewPass123!")).thenReturn("encoded-new");
        when(repository.save(any(UserAccount.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MessageResponse response = passwordResetService.changePassword("admin", "OldPass123!", "NewPass123!");

        assertThat(response.message()).contains("Contrasena actualizada");
        assertThat(account.getPassword()).isEqualTo("encoded-new");
        assertThat(account.isPasswordChangeRequired()).isFalse();
    }

    @Test
    void changePasswordShouldFailWhenCurrentMismatch() {
        UserAccount account = UserAccount.builder()
                .id("user-1")
                .username("admin")
                .password("encoded-old")
                .build();
        when(repository.findByUsernameIgnoreCase("admin")).thenReturn(Optional.of(account));
        when(passwordEncoder.matches("WrongPass!", "encoded-old")).thenReturn(false);

        assertThrows(BadRequestException.class,
                () -> passwordResetService.changePassword("admin", "WrongPass!", "NewPass123!"));
    }
}
