package com.financeapp.auth.service;

import com.financeapp.auth.entity.AuthUser;
import com.financeapp.auth.entity.RefreshToken;
import com.financeapp.auth.repository.AuthUserRepository;
import com.financeapp.common.JwtTokenProvider;
import com.financeapp.common.ServiceException;
import com.financeapp.dto.AuthTokenResponse;
import com.financeapp.auth.security.AuthenticationService;
import com.financeapp.auth.security.RateLimitingService;
import com.financeapp.auth.security.RefreshTokenService;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * DESIGN PATTERN: Service Layer Pattern
 * Encapsulates business logic with transactional support
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationService authenticationService;
    private final RateLimitingService rateLimitingService;
    private final AuthUserRepository authUserRepository;
    private final RefreshTokenService refreshTokenService;

    /**
     * DESIGN PATTERN: Retry Pattern with Circuit Breaker
     * Automatically retries failed authentication attempts
     */
    @Transactional
    @CircuitBreaker(name = "auth-service", fallbackMethod = "authenticateFallback")
    @Retry(name = "auth-service")
    public AuthTokenResponse authenticate(String email, String password) {
        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        
        // Rate limiting check
        if (!rateLimitingService.allowRequest(normalizedEmail)) {
            throw new ServiceException("Too many login attempts. Please try again later.", 
                                     "RATE_LIMIT_EXCEEDED", 429);
        }

        AuthUser user = authUserRepository.findByEmailIgnoreCase(normalizedEmail)
                .orElseThrow(() -> new ServiceException("Invalid credentials", "INVALID_CREDENTIALS", 401));

        if (!user.isActive()) {
            throw new ServiceException("User account is inactive", "USER_INACTIVE", 403);
        }

        authenticationService.validatePassword(password, user.getPasswordHash());
        List<String> roles = parseRoles(user.getRoles());

        String accessToken = jwtTokenProvider.generateAccessToken(
                user.getId(),
                user.getEmail(),
                roles
        );
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getId(), user.getEmail());
        refreshTokenService.store(
                refreshToken,
                user.getId(),
                jwtTokenProvider.extractTokenId(refreshToken),
                jwtTokenProvider.extractExpiration(refreshToken));

        log.info("User authenticated: {}", normalizedEmail);

        return AuthTokenResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(900) // 15 minutes
                .userId(user.getId())
                .email(user.getEmail())
                .roles(roles)
                .build();
    }

    @Transactional
    public AuthTokenResponse refreshToken(String refreshToken) {
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new ServiceException("Invalid refresh token", "INVALID_TOKEN", 401);
        }
        if (!"refresh".equals(jwtTokenProvider.extractTokenType(refreshToken))) {
            throw new ServiceException("Invalid refresh token", "INVALID_TOKEN", 401);
        }

        RefreshToken storedToken = refreshTokenService.requireActive(refreshToken);
        String userId = jwtTokenProvider.extractUserId(refreshToken);
        if (!storedToken.getUserId().equals(userId)) {
            throw new ServiceException("Invalid refresh token", "INVALID_TOKEN", 401);
        }

        AuthUser user = authUserRepository.findById(userId)
                .orElseThrow(() -> new ServiceException("User not found", "USER_NOT_FOUND", 404));
        if (!user.isActive()) {
            throw new ServiceException("User account is inactive", "USER_INACTIVE", 403);
        }

        List<String> roles = parseRoles(user.getRoles());

        String newAccessToken = jwtTokenProvider.generateAccessToken(
                user.getId(),
                user.getEmail(),
                roles
        );
        String newRefreshToken = jwtTokenProvider.generateRefreshToken(user.getId(), user.getEmail());
        refreshTokenService.rotate(
                storedToken,
                newRefreshToken,
                jwtTokenProvider.extractTokenId(newRefreshToken),
                jwtTokenProvider.extractExpiration(newRefreshToken));

        return AuthTokenResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .expiresIn(900)
                .userId(user.getId())
                .email(user.getEmail())
                .roles(roles)
                .build();
    }

    public void logout(String token) {
        if (jwtTokenProvider.validateToken(token)) {
            authenticationService.revokeToken(token);
            log.info("Token revoked");
        }
    }

    public boolean validateToken(String token) {
        if (authenticationService.isTokenRevoked(token)) {
            return false;
        }
        return jwtTokenProvider.validateToken(token)
                && "access".equals(jwtTokenProvider.extractTokenType(token));
    }

    public AuthTokenResponse authenticateFallback(String email, String password, Exception ex) {
        log.error("Authentication service fallback triggered: {}", ex.getMessage());
        throw new ServiceException("Authentication service unavailable", "SERVICE_UNAVAILABLE", 503);
    }

    private List<String> parseRoles(String roles) {
        return Stream.of(roles.split(","))
                .map(String::trim)
                .filter(role -> !role.isBlank())
                .map(role -> role.toUpperCase(Locale.ROOT))
                .collect(Collectors.toList());
    }
}
