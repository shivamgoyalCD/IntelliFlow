package com.shivam.intelliflow.auth.dto;

import java.time.Instant;

public record ValidateTokenResponse(
        boolean valid,
        String username,
        String role,
        Instant expiresAt
) {
}
