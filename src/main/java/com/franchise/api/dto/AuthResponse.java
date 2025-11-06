package com.franchise.api.dto;

import java.util.List;

public record AuthResponse(String token,
                           String username,
                           List<String> roles,
                           long expiresAt,
                           boolean passwordChangeRequired) {
}
