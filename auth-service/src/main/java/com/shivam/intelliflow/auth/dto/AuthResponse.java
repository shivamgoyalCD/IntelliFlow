package com.shivam.intelliflow.auth.dto;

import java.time.Instant;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresInSeconds,
        Instant expiresAt,
        String username
) {
}
