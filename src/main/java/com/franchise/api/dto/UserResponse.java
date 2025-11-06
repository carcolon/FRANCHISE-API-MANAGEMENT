package com.franchise.api.dto;

import java.util.List;

public record UserResponse(
        String id,
        String username,
        String fullName,
        String email,
        boolean active,
        List<String> roles
) {
}
