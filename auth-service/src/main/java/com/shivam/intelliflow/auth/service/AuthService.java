package com.shivam.intelliflow.auth.service;

import com.shivam.intelliflow.auth.dto.AuthResponse;
import com.shivam.intelliflow.auth.dto.LoginRequest;
import com.shivam.intelliflow.auth.dto.LogoutResponse;
import com.shivam.intelliflow.auth.dto.RefreshTokenRequest;
import com.shivam.intelliflow.auth.dto.ValidateTokenResponse;
import com.shivam.intelliflow.auth.entity.AuthUser;
import com.shivam.intelliflow.auth.publisher.AuthEventPublisher;
import com.shivam.intelliflow.auth.repository.AuthUserRepository;
import com.shivam.intelliflow.auth.security.JwtService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import java.util.Date;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {
    private static final String BEARER_PREFIX = "Bearer ";

    private final AuthUserRepository authUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthEventPublisher authEventPublisher;

    public AuthService(
            AuthUserRepository authUserRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            AuthEventPublisher authEventPublisher
    ) {
        this.authUserRepository = authUserRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authEventPublisher = authEventPublisher;
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        AuthUser user = authUserRepository.findByUsername(request.username())
                .orElse(null);

        if (user == null || !user.isEnabled() || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            authEventPublisher.loginFailed(request.username(), "invalid_credentials");
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username or password");
        }

        JwtService.TokenPair tokenPair = jwtService.issueTokenPair(user);
        authEventPublisher.loginSuccess(user);
        return toAuthResponse(tokenPair, user.getUsername());
    }

    @Transactional(readOnly = true)
    public LogoutResponse logout(String authorizationHeader) {
        String username = "anonymous";
        String token = extractBearerToken(authorizationHeader);

        if (token != null) {
            try {
                Claims claims = jwtService.validateAccessToken(token);
                username = claims.getSubject();
            } catch (JwtException | IllegalArgumentException exception) {
                username = "invalid_token";
            }
        }

        authEventPublisher.logout(username);
        return new LogoutResponse(true);
    }

    @Transactional(readOnly = true)
    public AuthResponse refresh(RefreshTokenRequest request) {
        try {
            Claims claims = jwtService.validateRefreshToken(request.refreshToken());
            AuthUser user = findEnabledUser(claims.getSubject());
            JwtService.TokenPair tokenPair = jwtService.issueTokenPair(user);
            authEventPublisher.tokenRefreshed(user);
            return toAuthResponse(tokenPair, user.getUsername());
        } catch (JwtException | IllegalArgumentException | ResponseStatusException exception) {
            authEventPublisher.validationFailed("unknown", "invalid_refresh_token");
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token");
        }
    }

    @Transactional(readOnly = true)
    public ValidateTokenResponse validate(String authorizationHeader) {
        String token = extractBearerToken(authorizationHeader);
        if (token == null) {
            authEventPublisher.validationFailed("anonymous", "missing_bearer_token");
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing bearer token");
        }

        try {
            Claims claims = jwtService.validateAccessToken(token);
            AuthUser user = findEnabledUser(claims.getSubject());
            Date expiration = claims.getExpiration();
            return new ValidateTokenResponse(
                    true,
                    user.getUsername(),
                    user.getRole(),
                    expiration == null ? null : expiration.toInstant()
            );
        } catch (JwtException | IllegalArgumentException | ResponseStatusException exception) {
            authEventPublisher.validationFailed("unknown", "invalid_access_token");
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid bearer token");
        }
    }

    private AuthUser findEnabledUser(String username) {
        AuthUser user = authUserRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid token subject"));
        if (!user.isEnabled()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User is disabled");
        }
        return user;
    }

    private AuthResponse toAuthResponse(JwtService.TokenPair tokenPair, String username) {
        return new AuthResponse(
                tokenPair.accessToken(),
                tokenPair.refreshToken(),
                "Bearer",
                tokenPair.accessTokenExpiresInSeconds(),
                tokenPair.accessTokenExpiresAt(),
                username
        );
    }

    private String extractBearerToken(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith(BEARER_PREFIX)) {
            return null;
        }
        String token = authorizationHeader.substring(BEARER_PREFIX.length()).trim();
        return token.isBlank() ? null : token;
    }
}
