package com.financeapp.dashboard.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.financeapp.common.Constants;
import com.financeapp.common.ServiceException;
import com.financeapp.common.ApiResponse;
import com.financeapp.dashboard.client.RecordPageResponse;
import com.financeapp.dashboard.client.RecordServiceClient;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * DESIGN PATTERN: CQRS (Command Query Responsibility Segregation) Pattern
 * Separates read operations from write operations
 * Uses cache for fast queries
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardCacheService {

    private final StringRedisTemplate redisTemplate;
    private final RecordServiceClient recordServiceClient;
    private final ObjectMapper objectMapper;

    @Value("${app.dashboard.cache-ttl-minutes:60}")
    private long cacheTtlMinutes;

    @Value("${app.dashboard.records-page-size:500}")
    private int recordsPageSize;

    @Value("${app.dashboard.records-max-pages:20}")
    private int recordsMaxPages;

    /**
     * DESIGN PATTERN: Read Model Pattern
     * Maintains a denormalized read model for fast analytics queries
     */
    public void invalidateSummaryCache(String userId) {
        String cacheKey = Constants.CACHE_DASHBOARD_SUMMARY + userId;
        try {
            redisTemplate.delete(cacheKey);
            log.info("Invalidated summary cache for user: {}", userId);
        } catch (Exception e) {
            log.error("Failed to invalidate summary cache: {}", e.getMessage());
        }
    }

    public void invalidateCategoryWiseCache(String userId) {
        String cacheKey = Constants.CACHE_DASHBOARD_CATEGORY_WISE + userId;
        try {
            redisTemplate.delete(cacheKey);
            log.info("Invalidated category-wise cache for user: {}", userId);
        } catch (Exception e) {
            log.error("Failed to invalidate category-wise cache: {}", e.getMessage());
        }
    }

    public DashboardSummary getSummary(String userId) {
        String cacheKey = Constants.CACHE_DASHBOARD_SUMMARY + userId;
        
        // Try to get from cache
        String cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            log.info("Cache hit for summary: {}", userId);
            try {
                return objectMapper.readValue(cached, DashboardSummary.class);
            } catch (JsonProcessingException e) {
                log.warn("Invalid cached dashboard summary for user {}, evicting entry", userId);
                redisTemplate.delete(cacheKey);
            }
        }

        // Cache miss - compute and store
        log.info("Cache miss for summary: {}", userId);
        DashboardSummary summary = computeSummary(userId);
        
        try {
            redisTemplate.opsForValue().set(
                    cacheKey, 
                    objectMapper.writeValueAsString(summary),
                    cacheTtlMinutes,
                    TimeUnit.MINUTES
            );
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize dashboard summary: {}", e.getMessage());
        } catch (RuntimeException e) {
            log.error("Failed to cache summary: {}", e.getMessage());
        }

        return summary;
    }

    private DashboardSummary computeSummary(String userId) {
        BigDecimal totalIncome = BigDecimal.ZERO;
        BigDecimal totalExpense = BigDecimal.ZERO;
        int page = 0;
        int totalPages;

        do {
            ApiResponse<RecordPageResponse> response = recordServiceClient.getRecords(
                    userId,
                    page,
                    Math.max(1, recordsPageSize));

            if (response == null || !response.isSuccess() || response.getData() == null) {
                throw new ServiceException("Unable to compute dashboard summary", "RECORD_SERVICE_UNAVAILABLE", 503);
            }

            RecordPageResponse pageResponse = response.getData();
            List<RecordPageResponse.RecordResponse> records = pageResponse.getContent();
            for (RecordPageResponse.RecordResponse record : records) {
                if (record.isDeleted() || record.getAmount() == null || record.getType() == null) {
                    continue;
                }
                if ("INCOME".equalsIgnoreCase(record.getType())) {
                    totalIncome = totalIncome.add(record.getAmount());
                } else if ("EXPENSE".equalsIgnoreCase(record.getType())) {
                    totalExpense = totalExpense.add(record.getAmount());
                }
            }

            totalPages = pageResponse.getTotalPages();
            page++;
        } while (page < totalPages && page < Math.max(1, recordsMaxPages));

        return DashboardSummary.builder()
                .totalIncome(totalIncome)
                .totalExpense(totalExpense)
                .netBalance(totalIncome.subtract(totalExpense))
                .build();
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DashboardSummary {
        @JsonProperty("total_income")
        private BigDecimal totalIncome;
        
        @JsonProperty("total_expense")
        private BigDecimal totalExpense;
        
        @JsonProperty("net_balance")
        private BigDecimal netBalance;
    }
}
