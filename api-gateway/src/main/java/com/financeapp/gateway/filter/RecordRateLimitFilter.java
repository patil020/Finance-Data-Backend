package com.financeapp.gateway.filter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.financeapp.common.ApiResponse;
import com.financeapp.common.Constants;
import com.financeapp.common.ErrorDetails;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Slf4j
@Component
public class RecordRateLimitFilter extends AbstractGatewayFilterFactory<RecordRateLimitFilter.Config> {

    private static final String REDIS_KEY_PREFIX = "rate-limit:records:";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final Map<String, BucketState> localBuckets = new ConcurrentHashMap<>();

    @Value("${app.rate-limit.records.capacity:60}")
    private long capacity;

    @Value("${app.rate-limit.records.refill-tokens:60}")
    private long refillTokens;

    @Value("${app.rate-limit.records.refill-period-seconds:60}")
    private long refillPeriodSeconds;

    @Value("${app.rate-limit.records.redis-enabled:true}")
    private boolean redisEnabled;

    public RecordRateLimitFilter(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        super(Config.class);
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            String principal = exchange.getRequest().getHeaders().getFirst("X-User-Id");
            if (principal == null || principal.isBlank()) {
                principal = exchange.getRequest().getRemoteAddress() == null
                        ? "unknown"
                        : exchange.getRequest().getRemoteAddress().getAddress().getHostAddress();
            }

            RateLimitDecision decision = consume(principal);
            exchange.getResponse().getHeaders().set("X-RateLimit-Limit", String.valueOf(capacity));
            exchange.getResponse().getHeaders().set("X-RateLimit-Remaining", String.valueOf(decision.remainingTokens()));

            if (!decision.allowed()) {
                exchange.getResponse().getHeaders().set("Retry-After", String.valueOf(refillPeriodSeconds));
                log.warn("Record API rate limit exceeded for {}", principal);
                return tooManyRequests(exchange);
            }

            return chain.filter(exchange);
        };
    }

    private RateLimitDecision consume(String principal) {
        if (redisEnabled) {
            try {
                return consumeRedis(principal);
            } catch (RuntimeException ex) {
                log.error("Redis rate limit check failed, using local fallback: {}", ex.getMessage());
            }
        }
        return consumeLocal(principal);
    }

    private RateLimitDecision consumeRedis(String principal) {
        String redisKey = REDIS_KEY_PREFIX + principal;
        BucketState bucket = readBucket(redisKey);
        RateLimitDecision decision = consume(bucket);
        redisTemplate.opsForValue().set(
                redisKey,
                writeBucket(bucket),
                Math.max(refillPeriodSeconds * 2, 120),
                java.util.concurrent.TimeUnit.SECONDS);
        return decision;
    }

    private RateLimitDecision consumeLocal(String principal) {
        BucketState bucket = localBuckets.computeIfAbsent(
                principal,
                ignored -> new BucketState(capacity, nowSeconds()));
        synchronized (bucket) {
            return consume(bucket);
        }
    }

    private RateLimitDecision consume(BucketState bucket) {
        refill(bucket);
        if (bucket.getTokens() < 1) {
            return new RateLimitDecision(false, bucket.getTokens());
        }
        bucket.setTokens(bucket.getTokens() - 1);
        return new RateLimitDecision(true, bucket.getTokens());
    }

    private void refill(BucketState bucket) {
        long now = nowSeconds();
        long elapsed = now - bucket.getLastRefillEpochSecond();
        if (elapsed < refillPeriodSeconds) {
            return;
        }
        long periods = elapsed / refillPeriodSeconds;
        long tokensToAdd = periods * refillTokens;
        bucket.setTokens(Math.min(capacity, bucket.getTokens() + tokensToAdd));
        bucket.setLastRefillEpochSecond(bucket.getLastRefillEpochSecond() + (periods * refillPeriodSeconds));
    }

    private BucketState readBucket(String redisKey) {
        String rawBucket = redisTemplate.opsForValue().get(redisKey);
        if (rawBucket == null) {
            return new BucketState(capacity, nowSeconds());
        }
        try {
            return objectMapper.readValue(rawBucket, BucketState.class);
        } catch (JsonProcessingException ex) {
            return new BucketState(capacity, nowSeconds());
        }
    }

    private String writeBucket(BucketState bucket) {
        try {
            return objectMapper.writeValueAsString(bucket);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize record rate-limit bucket", ex);
        }
    }

    private long nowSeconds() {
        return System.currentTimeMillis() / 1000;
    }

    private Mono<Void> tooManyRequests(ServerWebExchange exchange) {
        LocalDateTime timestamp = LocalDateTime.now();
        String correlationId = exchange.getRequest().getHeaders().getFirst(Constants.CORRELATION_ID_HEADER);
        ErrorDetails errorDetails = ErrorDetails.builder()
                .code("RATE_LIMIT_EXCEEDED")
                .message("Too many record API requests")
                .status(HttpStatus.TOO_MANY_REQUESTS.value())
                .path(exchange.getRequest().getPath().value())
                .timestamp(timestamp)
                .build();
        ApiResponse<Void> responseBody = ApiResponse.<Void>builder()
                .success(false)
                .message("Too many record API requests")
                .error(errorDetails)
                .timestamp(timestamp)
                .correlationId(correlationId)
                .build();

        exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        try {
            byte[] bytes = objectMapper.writeValueAsBytes(responseBody);
            DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);
            return exchange.getResponse().writeWith(Mono.just(buffer));
        } catch (JsonProcessingException ex) {
            log.error("Failed to serialize rate-limit response", ex);
            return exchange.getResponse().setComplete();
        }
    }

    private record RateLimitDecision(boolean allowed, long remainingTokens) {
    }

    @Data
    public static class BucketState {
        private long tokens;
        private long lastRefillEpochSecond;

        public BucketState() {
        }

        public BucketState(long tokens, long lastRefillEpochSecond) {
            this.tokens = tokens;
            this.lastRefillEpochSecond = lastRefillEpochSecond;
        }
    }

    public static class Config {
    }
}
