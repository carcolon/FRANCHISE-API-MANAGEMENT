package com.franchise.api.security;
import com.franchise.api.dto.ForgotPasswordResponse;
import com.franchise.api.dto.MessageResponse;
import com.franchise.api.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PasswordResetService {

    private static final Duration TOKEN_TTL = Duration.ofMinutes(15);
    private static final String GENERIC_MESSAGE = "Si la cuenta existe recibiras instrucciones para restablecer tu contrasena.";

    private final UserAccountRepository repository;
    private final PasswordEncoder passwordEncoder;

    public ForgotPasswordResponse initiateReset(String username) {
        return repository.findByUsernameIgnoreCase(username)
                .map(user -> {
                    String token = UUID.randomUUID().toString();
                    user.setPasswordResetToken(token);
                    user.setPasswordResetExpiration(Instant.now().plus(TOKEN_TTL));
                    repository.save(user);
                    log.info("Token de restablecimiento generado para {}: {}", user.getUsername(), token);
                    return new ForgotPasswordResponse(GENERIC_MESSAGE, token);
                })
                .orElseGet(() -> {
                    log.warn("Solicitud de restablecimiento para usuario no encontrado: {}", username);
                    return new ForgotPasswordResponse(GENERIC_MESSAGE, null);
                });
    }

    public MessageResponse resetPassword(String token, String newPassword) {
        UserAccount account = repository.findByPasswordResetToken(token)
                .orElseThrow(() -> new BadRequestException("Token de restablecimiento invalido"));
        if (account.getPasswordResetExpiration() == null || account.getPasswordResetExpiration().isBefore(Instant.now())) {
            throw new BadRequestException("El token de restablecimiento ha expirado");
        }
        account.setPassword(passwordEncoder.encode(newPassword));
        account.setPasswordResetToken(null);
        account.setPasswordResetExpiration(null);
        account.setPasswordChangeRequired(false);
        repository.save(account);
        return new MessageResponse("Contrasena actualizada correctamente.");
    }

    public MessageResponse validateToken(String token) {
        UserAccount account = repository.findByPasswordResetToken(token)
                .orElseThrow(() -> new BadRequestException("Token de restablecimiento invalido"));
        if (account.getPasswordResetExpiration() == null || account.getPasswordResetExpiration().isBefore(Instant.now())) {
            throw new BadRequestException("El token de restablecimiento ha expirado");
        }
        return new MessageResponse("Token valido. Puedes definir una nueva contrasena.");
    }

    public MessageResponse changePassword(String username, String currentPassword, String newPassword) {
        UserAccount account = repository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new BadRequestException("Usuario no encontrado"));
        if (!passwordEncoder.matches(currentPassword, account.getPassword())) {
            throw new BadRequestException("La contrasena actual no es correcta");
        }
        account.setPassword(passwordEncoder.encode(newPassword));
        account.setPasswordChangeRequired(false);
        repository.save(account);
        return new MessageResponse("Contrasena actualizada correctamente.");
    }
}
