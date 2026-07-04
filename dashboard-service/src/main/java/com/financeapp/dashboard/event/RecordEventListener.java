package com.financeapp.dashboard.event;

import com.financeapp.common.Constants;
import com.financeapp.event.FinancialRecordEvent;
import com.financeapp.dashboard.service.DashboardCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

/**
 * DESIGN PATTERN: Event Consumer/Subscriber Pattern
 * Listens to events published by Record Service
 * Used for asynchronous, loosely-coupled communication
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RecordEventListener {

    private final DashboardCacheService dashboardCacheService;

    /**
     * DESIGN PATTERN: Event-Driven Cache Invalidation
     * Listens to record creation and invalidates dashboard cache
     */
    @KafkaListener(topics = Constants.KAFKA_RECORD_CREATED_TOPIC, groupId = "dashboard-service")
    public void onRecordCreated(FinancialRecordEvent event) {
        log.info("Record created event received: {}", event.getRecordId());
        try {
            dashboardCacheService.invalidateSummaryCache(event.getUserId());
            dashboardCacheService.invalidateCategoryWiseCache(event.getUserId());
        } catch (Exception e) {
            log.error("Failed to invalidate cache: {}", e.getMessage());
        }
    }

    @KafkaListener(topics = Constants.KAFKA_RECORD_UPDATED_TOPIC, groupId = "dashboard-service")
    public void onRecordUpdated(FinancialRecordEvent event) {
        log.info("Record updated event received: {}", event.getRecordId());
        try {
            dashboardCacheService.invalidateSummaryCache(event.getUserId());
            dashboardCacheService.invalidateCategoryWiseCache(event.getUserId());
        } catch (Exception e) {
            log.error("Failed to invalidate cache: {}", e.getMessage());
        }
    }

    @KafkaListener(topics = Constants.KAFKA_RECORD_DELETED_TOPIC, groupId = "dashboard-service")
    public void onRecordDeleted(FinancialRecordEvent event) {
        log.info("Record deleted event received: {}", event.getRecordId());
        try {
            dashboardCacheService.invalidateSummaryCache(event.getUserId());
            dashboardCacheService.invalidateCategoryWiseCache(event.getUserId());
        } catch (Exception e) {
            log.error("Failed to invalidate cache: {}", e.getMessage());
        }
    }
}
