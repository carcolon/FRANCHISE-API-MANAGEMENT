package com.franchise.api.controller;

import com.franchise.api.dto.AuthRequest;
import com.franchise.api.dto.AuthResponse;
import com.franchise.api.dto.ForgotPasswordRequest;
import com.franchise.api.dto.ForgotPasswordResponse;
import com.franchise.api.dto.MessageResponse;
import com.franchise.api.dto.ResetPasswordRequest;
import com.franchise.api.dto.ValidateTokenRequest;
import com.franchise.api.dto.ChangePasswordRequest;
import com.franchise.api.security.JwtService;
import com.franchise.api.security.PasswordResetService;
import com.franchise.api.security.UserAccount;
import com.franchise.api.security.UserAccountRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final PasswordResetService passwordResetService;
    private final UserAccountRepository userAccountRepository;

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody AuthRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password())
        );
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String token = jwtService.generateToken(userDetails);
        long expiresAt = System.currentTimeMillis() + jwtService.getExpirationInMs();
        List<String> roles = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .map(authority -> authority.replaceFirst("^ROLE_", ""))
                .toList();
        boolean passwordChangeRequired = userAccountRepository.findByUsernameIgnoreCase(userDetails.getUsername())
                .map(UserAccount::isPasswordChangeRequired)
                .orElse(false);
        return ResponseEntity.ok(new AuthResponse(token, userDetails.getUsername(), roles, expiresAt, passwordChangeRequired));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ForgotPasswordResponse> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        return ResponseEntity.ok(passwordResetService.initiateReset(request.username()));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<MessageResponse> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        return ResponseEntity.ok(passwordResetService.resetPassword(request.token(), request.newPassword()));
    }

    @PostMapping("/validate-reset-token")
    public ResponseEntity<MessageResponse> validateResetToken(@Valid @RequestBody ValidateTokenRequest request) {
        return ResponseEntity.ok(passwordResetService.validateToken(request.token()));
    }

    @PostMapping("/change-password")
    public ResponseEntity<MessageResponse> changePassword(@Valid @RequestBody ChangePasswordRequest request,
                                                          Authentication authentication) {
        return ResponseEntity.ok(passwordResetService.changePassword(authentication.getName(),
                request.currentPassword(), request.newPassword()));
    }
}

/* test commit
 */