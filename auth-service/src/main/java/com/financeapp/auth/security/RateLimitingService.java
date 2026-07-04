package com.financeapp.auth.security;

import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

/**
 * Distributed token-bucket rate limiter for authentication endpoints.
 */
@Slf4j
@Service
public class RateLimitingService {

    private static final String KEY_PREFIX = "rate-limit:auth:";
    private static final int REQUEST_COST = 1;

    private static final DefaultRedisScript<List> TOKEN_BUCKET_SCRIPT = new DefaultRedisScript<>("""
            local key = KEYS[1]
            local capacity = tonumber(ARGV[1])
            local refill_tokens = tonumber(ARGV[2])
            local refill_period_ms = tonumber(ARGV[3])
            local now_ms = tonumber(ARGV[4])
            local requested = tonumber(ARGV[5])
            local ttl_ms = tonumber(ARGV[6])

            local values = redis.call('HMGET', key, 'tokens', 'updated_at')
            local tokens = tonumber(values[1])
            local updated_at = tonumber(values[2])

            if tokens == nil or updated_at == nil then
              tokens = capacity
              updated_at = now_ms
            else
              local elapsed = math.max(0, now_ms - updated_at)
              local periods = math.floor(elapsed / refill_period_ms)
              if periods > 0 then
                tokens = math.min(capacity, tokens + (periods * refill_tokens))
                updated_at = updated_at + (periods * refill_period_ms)
              end
            end

            local allowed = 0
            if tokens >= requested then
              tokens = tokens - requested
              allowed = 1
            end

            redis.call('HSET', key, 'tokens', tokens, 'updated_at', updated_at)
            redis.call('PEXPIRE', key, ttl_ms)
            return { allowed, tokens }
            """, List.class);

    private final StringRedisTemplate redisTemplate;
    private final Map<String, LocalBucket> localBuckets = new ConcurrentHashMap<>();

    @Value("${app.rate-limit.auth.capacity:10}")
    private long capacity;

    @Value("${app.rate-limit.auth.refill-tokens:10}")
    private long refillTokens;

    @Value("${app.rate-limit.auth.refill-period-seconds:60}")
    private long refillPeriodSeconds;

    @Value("${app.rate-limit.auth.redis-enabled:true}")
    private boolean redisEnabled;

    public RateLimitingService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public boolean allowRequest(String identifier) {
        String normalizedIdentifier = normalize(identifier);
        boolean allowed = redisEnabled
                ? allowWithRedis(normalizedIdentifier)
                : allowWithLocalBucket(normalizedIdentifier);

        if (!allowed) {
            log.warn("Rate limit exceeded for identifier: {}", normalizedIdentifier);
        }
        return allowed;
    }

    public long getRemainingTokens(String identifier) {
        LocalBucket bucket = localBuckets.get(normalize(identifier));
        if (bucket == null) {
            return capacity;
        }
        synchronized (bucket) {
            refill(bucket, System.currentTimeMillis());
            return bucket.tokens;
        }
    }

    private boolean allowWithRedis(String identifier) {
        long refillPeriodMillis = Duration.ofSeconds(Math.max(1, refillPeriodSeconds)).toMillis();
        long ttlMillis = Math.max(refillPeriodMillis * 2, Duration.ofMinutes(5).toMillis());

        try {
            List<?> result = redisTemplate.execute(
                    TOKEN_BUCKET_SCRIPT,
                    List.of(KEY_PREFIX + identifier),
                    Long.toString(capacity),
                    Long.toString(refillTokens),
                    Long.toString(refillPeriodMillis),
                    Long.toString(System.currentTimeMillis()),
                    Integer.toString(REQUEST_COST),
                    Long.toString(ttlMillis));

            return result != null && !result.isEmpty() && asLong(result.get(0)) == 1L;
        } catch (RuntimeException ex) {
            log.warn("Redis rate limit check failed, using local fallback: {}", ex.getMessage());
            return allowWithLocalBucket(identifier);
        }
    }

    private boolean allowWithLocalBucket(String identifier) {
        LocalBucket bucket = localBuckets.computeIfAbsent(identifier, key -> new LocalBucket(capacity));
        synchronized (bucket) {
            refill(bucket, System.currentTimeMillis());
            if (bucket.tokens < REQUEST_COST) {
                return false;
            }
            bucket.tokens -= REQUEST_COST;
            return true;
        }
    }

    private void refill(LocalBucket bucket, long nowMillis) {
        long refillPeriodMillis = Duration.ofSeconds(Math.max(1, refillPeriodSeconds)).toMillis();
        long elapsed = Math.max(0, nowMillis - bucket.updatedAtMillis);
        long periods = elapsed / refillPeriodMillis;
        if (periods <= 0) {
            return;
        }
        bucket.tokens = Math.min(capacity, bucket.tokens + (periods * refillTokens));
        bucket.updatedAtMillis += periods * refillPeriodMillis;
    }

    private String normalize(String identifier) {
        if (identifier == null || identifier.isBlank()) {
            return "anonymous";
        }
        return identifier.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9@._:-]", "_");
    }

    private long asLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.parseLong(String.valueOf(value));
    }

    private static final class LocalBucket {
        private long tokens;
        private long updatedAtMillis;

        private LocalBucket(long capacity) {
            this.tokens = capacity;
            this.updatedAtMillis = System.currentTimeMillis();
        }
    }
}
