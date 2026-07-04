package com.financeapp.gateway.filter;

import com.financeapp.common.Constants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import java.util.UUID;

/**
 * DESIGN PATTERN: Filter/Interceptor Pattern
 * Adds correlation ID to all requests for distributed tracing
 */
@Slf4j
@Component
public class CorrelationIdFilter extends AbstractGatewayFilterFactory<CorrelationIdFilter.Config> {

    public CorrelationIdFilter() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            String correlationId = exchange.getRequest()
                    .getHeaders()
                    .getFirst(Constants.CORRELATION_ID_HEADER);

            if (correlationId == null) {
                correlationId = UUID.randomUUID().toString();
            }

            String finalCorrelationId = correlationId;
            ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                    .header(Constants.CORRELATION_ID_HEADER, finalCorrelationId)
                    .build();
            exchange.getResponse().getHeaders().set(Constants.CORRELATION_ID_HEADER, finalCorrelationId);

            log.info("Request with correlation ID: {}", finalCorrelationId);

            return chain.filter(exchange.mutate().request(mutatedRequest).build());
        };
    }

    public static class Config {
    }
}
