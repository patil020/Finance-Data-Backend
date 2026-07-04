package com.financeapp.auth.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import java.util.concurrent.TimeUnit;

/**
 * DESIGN PATTERN: Token Blacklist Pattern
 * Maintains a distributed blacklist of revoked tokens using Redis
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TokenBlacklistService {

    private final StringRedisTemplate redisTemplate;
    private static final String BLACKLIST_PREFIX = "token:blacklist:";
    private static final long EXPIRATION_TIME = 24; // hours

    public void addToBlacklist(String token) {
        try {
            redisTemplate.opsForValue().set(
                    BLACKLIST_PREFIX + token, 
                    "revoked", 
                    EXPIRATION_TIME, 
                    TimeUnit.HOURS
            );
            log.info("Token added to blacklist");
        } catch (Exception e) {
            log.error("Failed to add token to blacklist: {}", e.getMessage());
        }
    }

    public boolean isBlacklisted(String token) {
        try {
            Boolean exists = redisTemplate.hasKey(BLACKLIST_PREFIX + token);
            return exists != null && exists;
        } catch (Exception e) {
            log.error("Failed to check token blacklist: {}", e.getMessage());
            return false;
        }
    }
}
