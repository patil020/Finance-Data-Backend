package com.financeapp.gateway.filter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.financeapp.common.Constants;
import com.financeapp.common.ApiResponse;
import com.financeapp.common.ErrorDetails;
import com.financeapp.common.JwtTokenProvider;
import java.time.LocalDateTime;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import java.util.List;

/**
 * DESIGN PATTERN: Authentication Filter Pattern
 * Validates JWT tokens before routing to microservices
 */
@Slf4j
@Component
public class AuthenticationFilter extends AbstractGatewayFilterFactory<AuthenticationFilter.Config> {

    private final JwtTokenProvider jwtTokenProvider;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public AuthenticationFilter(
            JwtTokenProvider jwtTokenProvider,
            StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper) {
        super(Config.class);
        this.jwtTokenProvider = jwtTokenProvider;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            String authHeader = exchange.getRequest()
                    .getHeaders()
                    .getFirst(Constants.AUTHORIZATION_HEADER);

            if (authHeader == null || !authHeader.startsWith(Constants.BEARER_PREFIX)) {
                log.warn("Missing or invalid authorization header");
                return unauthorized(exchange, "Missing or invalid Authorization header");
            }

            String token = authHeader.substring(Constants.BEARER_PREFIX.length());

            // Check if token is blacklisted
            if (isTokenBlacklisted(token)) {
                log.warn("Token is blacklisted");
                return unauthorized(exchange, "Token has been revoked");
            }

            // Validate token
            if (!jwtTokenProvider.validateToken(token)) {
                log.warn("Invalid token");
                return unauthorized(exchange, "Invalid or expired token");
            }
            if (!"access".equals(jwtTokenProvider.extractTokenType(token))) {
                log.warn("Non-access token rejected at gateway");
                return unauthorized(exchange, "Invalid token type");
            }

            // Extract user info and add to headers
            String userId = jwtTokenProvider.extractUserId(token);
            String email = jwtTokenProvider.extractEmail(token);
            List<String> roles = jwtTokenProvider.extractRoles(token);
            ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                    .header("X-User-Id", userId)
                    .header("X-User-Email", email == null ? "" : email)
                    .header("X-User-Roles", roles == null ? "" : String.join(",", roles))
                    .build();

            log.info("Token validated for user: {}", userId);
            return chain.filter(exchange.mutate().request(mutatedRequest).build());
        };
    }

    private boolean isTokenBlacklisted(String token) {
        try {
            return redisTemplate.hasKey("token:blacklist:" + token) == Boolean.TRUE;
        } catch (Exception e) {
            log.error("Error checking token blacklist: {}", e.getMessage());
            return false;
        }
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange, String message) {
        LocalDateTime timestamp = LocalDateTime.now();
        String correlationId = exchange.getRequest().getHeaders().getFirst(Constants.CORRELATION_ID_HEADER);
        ErrorDetails errorDetails = ErrorDetails.builder()
                .code(Constants.ERR_UNAUTHORIZED)
                .message(message)
                .status(HttpStatus.UNAUTHORIZED.value())
                .path(exchange.getRequest().getPath().value())
                .timestamp(timestamp)
                .build();
        ApiResponse<Void> responseBody = ApiResponse.<Void>builder()
                .success(false)
                .message(message)
                .error(errorDetails)
                .timestamp(timestamp)
                .correlationId(correlationId)
                .build();

        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        try {
            byte[] bytes = objectMapper.writeValueAsBytes(responseBody);
            DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);
            return exchange.getResponse().writeWith(Mono.just(buffer));
        } catch (JsonProcessingException ex) {
            log.error("Failed to serialize gateway error response", ex);
            return exchange.getResponse().setComplete();
        }
    }

    public static class Config {
    }
}
