package com.financeapp.security;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * Service for tracking login attempts and implementing rate limiting.
 * Uses Guava's LoadingCache to track failed attempts per email address.
 * 
 * Rate limiting parameters:
 * - Max failed attempts: 5
 * - Lockout duration: 15 minutes
 */
@Service
@Slf4j
public class LoginAttemptService {
    
    private static final int MAX_ATTEMPT = 5;
    private static final int ATTEMPT_INCREMENT = 1;
    private static final long LOCKOUT_DURATION_MINUTES = 15;
    
    private final LoadingCache<String, Integer> attemptsCache;
    
    public LoginAttemptService() {
        super();
        attemptsCache = CacheBuilder.newBuilder()
                .expireAfterWrite(LOCKOUT_DURATION_MINUTES, TimeUnit.MINUTES)
                .build(new CacheLoader<String, Integer>() {
                    @Override
                    public Integer load(String key) {
                        return 0;
                    }
                });
    }
    
    /**
     * Record a successful login - clears attempt count
     */
    public void loginSucceeded(String email) {
        attemptsCache.invalidate(email);
        log.debug("Login succeeded for email: {} - attempt count reset", email);
    }
    
    /**
     * Record a failed login attempt
     */
    public void loginFailed(String email) {
        int attempts = 0;
        try {
            attempts = attemptsCache.get(email);
        } catch (ExecutionException e) {
            attempts = 0;
        }
        attempts++;
        attemptsCache.put(email, attempts);
        log.warn("Failed login attempt for email: {} - attempt #{}", email, attempts);
    }
    
    /**
     * Check if user is locked out due to too many attempts
     */
    public boolean isAccountLocked(String email) {
        try {
            return attemptsCache.get(email) >= MAX_ATTEMPT;
        } catch (ExecutionException e) {
            return false;
        }
    }
    
    /**
     * Get remaining attempts before lockout
     */
    public int getRemainingAttempts(String email) {
        try {
            int attempts = attemptsCache.get(email);
            return Math.max(0, MAX_ATTEMPT - attempts);
        } catch (ExecutionException e) {
            return MAX_ATTEMPT;
        }
    }
}
