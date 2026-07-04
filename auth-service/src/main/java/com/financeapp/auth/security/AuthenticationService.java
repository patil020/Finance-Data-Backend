package com.financeapp.auth.security;

import com.financeapp.common.ServiceException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.util.UUID;

/**
 * DESIGN PATTERN: Service Pattern
 * Handles authentication logic with resilience patterns
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private final PasswordEncoder passwordEncoder;
    private final TokenBlacklistService tokenBlacklistService;

    /**
     * DESIGN PATTERN: Circuit Breaker Pattern
     * Prevents cascading failures when external services fail
     */
    @CircuitBreaker(name = "auth-service", fallbackMethod = "authenticateFallback")
    @Retry(name = "auth-service")
    public void validatePassword(String rawPassword, String encodedPassword) {
        if (!passwordEncoder.matches(rawPassword, encodedPassword)) {
            throw new ServiceException("Invalid credentials", "INVALID_CREDENTIALS", 401);
        }
    }

    /**
     * DESIGN PATTERN: Fallback Pattern
     * Provides graceful degradation when circuit breaker is open
     */
    public void authenticateFallback(String rawPassword, String encodedPassword, Exception ex) {
        log.error("Circuit breaker fallback triggered for auth: {}", ex.getMessage());
        throw new ServiceException("Authentication service temporarily unavailable", 
                                 "SERVICE_UNAVAILABLE", 503);
    }

    public String generateSessionId() {
        return UUID.randomUUID().toString();
    }

    public void revokeToken(String token) {
        tokenBlacklistService.addToBlacklist(token);
    }

    public boolean isTokenRevoked(String token) {
        return tokenBlacklistService.isBlacklisted(token);
    }
}
