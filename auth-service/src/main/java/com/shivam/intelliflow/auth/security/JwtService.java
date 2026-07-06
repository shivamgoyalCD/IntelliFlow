package com.shivam.intelliflow.auth.security;

import com.shivam.intelliflow.auth.entity.AuthUser;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtService {
    public static final String ACCESS_TOKEN_TYPE = "access";
    public static final String REFRESH_TOKEN_TYPE = "refresh";

    private static final String TOKEN_TYPE_CLAIM = "token_type";
    private static final String ROLE_CLAIM = "role";

    private final SecretKey signingKey;
    private final Duration accessTokenTtl;
    private final Duration refreshTokenTtl;
    private final String issuer;

    public JwtService(
            @Value("${intelliflow.auth.jwt.secret}") String secret,
            @Value("${intelliflow.auth.jwt.access-token-ttl:15m}") Duration accessTokenTtl,
            @Value("${intelliflow.auth.jwt.refresh-token-ttl:7d}") Duration refreshTokenTtl,
            @Value("${intelliflow.auth.jwt.issuer:intelliflow-auth-service}") String issuer
    ) {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenTtl = accessTokenTtl;
        this.refreshTokenTtl = refreshTokenTtl;
        this.issuer = issuer;
    }

    public TokenPair issueTokenPair(AuthUser user) {
        Instant accessExpiresAt = Instant.now().plus(accessTokenTtl);
        Instant refreshExpiresAt = Instant.now().plus(refreshTokenTtl);
        return new TokenPair(
                generateToken(user, ACCESS_TOKEN_TYPE, accessExpiresAt),
                generateToken(user, REFRESH_TOKEN_TYPE, refreshExpiresAt),
                accessExpiresAt,
                accessTokenTtl.toSeconds()
        );
    }

    public Claims validateAccessToken(String token) {
        Claims claims = parseClaims(token);
        ensureTokenType(claims, ACCESS_TOKEN_TYPE);
        return claims;
    }

    public Claims validateRefreshToken(String token) {
        Claims claims = parseClaims(token);
        ensureTokenType(claims, REFRESH_TOKEN_TYPE);
        return claims;
    }

    private String generateToken(AuthUser user, String tokenType, Instant expiresAt) {
        Instant now = Instant.now();
        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .issuer(issuer)
                .subject(user.getUsername())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .claim(TOKEN_TYPE_CLAIM, tokenType)
                .claim(ROLE_CLAIM, user.getRole())
                .signWith(signingKey)
                .compact();
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .requireIssuer(issuer)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private void ensureTokenType(Claims claims, String expectedTokenType) {
        String tokenType = claims.get(TOKEN_TYPE_CLAIM, String.class);
        if (!expectedTokenType.equals(tokenType)) {
            throw new JwtException("Unexpected token type");
        }
    }

    public record TokenPair(
            String accessToken,
            String refreshToken,
            Instant accessTokenExpiresAt,
            long accessTokenExpiresInSeconds
    ) {
    }
}
