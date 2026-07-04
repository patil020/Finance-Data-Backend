package com.financeapp.record.service;

import com.financeapp.common.ServiceException;
import com.financeapp.common.Constants;
import com.financeapp.event.FinancialRecordEvent;
import com.financeapp.record.entity.FinancialRecord;
import com.financeapp.record.outbox.OutboxService;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DESIGN PATTERN: Service Layer Pattern with Saga Orchestration
 * Orchestrates the creation of financial records using Saga pattern
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RecordService {

    private final FinancialRecordRepository recordRepository;
    private final OutboxService outboxService;

    /**
     * DESIGN PATTERN: Saga Orchestration Pattern
     * Orchestrates multi-step transaction with compensation
     */
    @Transactional
    @CircuitBreaker(name = "record-service", fallbackMethod = "createRecordFallback")
    public FinancialRecord createRecord(String userId, BigDecimal amount, 
                                       String type, String category, String description, LocalDate date) {
        FinancialRecord record = new FinancialRecord();
        record.setUserId(userId);
        record.setAmount(amount);
        record.setType(parseRecordType(type));
        record.setCategory(category.trim());
        record.setDescription(description == null ? null : description.trim());
        record.setDate(date == null ? LocalDate.now() : date);
        record.setCreatedAt(LocalDateTime.now());
        record.setUpdatedAt(LocalDateTime.now());

        FinancialRecord savedRecord = recordRepository.save(record);
        outboxService.enqueueFinancialRecordEvent(
                Constants.KAFKA_RECORD_CREATED_TOPIC,
                buildRecordEvent(savedRecord, "CREATE", UUID.randomUUID().toString()));

        log.info("Record created and outbox event enqueued: {}", savedRecord.getId());
        return savedRecord;
    }

    /**
     * DESIGN PATTERN: Cache-Aside Pattern
     * Caches frequently accessed records
     */
    @Cacheable(value = "records", key = "#userId + ':' + #page + ':' + #size")
    public Page<FinancialRecord> getRecordsByUser(String userId, int page, int size) {
        return recordRepository.findByUserIdAndDeletedFalse(userId, PageRequest.of(page, size));
    }

    @Cacheable(value = "record", key = "#userId + ':' + #recordId")
    public FinancialRecord getRecordById(String userId, String recordId) {
        return recordRepository.findByIdAndUserIdAndDeletedFalse(recordId, userId)
                .orElseThrow(() -> new ServiceException("Record not found", "NOT_FOUND", 404));
    }

    @Cacheable(value = "records", key = "'filter:' + #userId + ':' + #category + ':' + #type + ':' + #fromDate + ':' + #toDate + ':' + #page + ':' + #size")
    public Page<FinancialRecord> filterRecords(
            String userId,
            String category,
            String type,
            LocalDate fromDate,
            LocalDate toDate,
            int page,
            int size) {
        if (fromDate != null && toDate != null && fromDate.isAfter(toDate)) {
            throw new ServiceException("fromDate must be before or equal to toDate", "INVALID_DATE_RANGE", 400);
        }

        String normalizedCategory = category == null || category.isBlank() ? null : category.trim();
        FinancialRecord.RecordType recordType = type == null || type.isBlank() ? null : parseRecordType(type);

        return recordRepository.filterRecords(
                userId,
                normalizedCategory,
                recordType,
                fromDate,
                toDate,
                PageRequest.of(page, size));
    }

    /**
     * DESIGN PATTERN: Event-Driven Cache Invalidation
     * Clears cache when record is updated
     */
    @Transactional
    @CacheEvict(value = {"records", "record"}, allEntries = true)
    public FinancialRecord updateRecord(String userId, String recordId, BigDecimal amount, String category) {
        FinancialRecord record = getRecordById(userId, recordId);
        record.setAmount(amount);
        record.setCategory(category.trim());
        record.setUpdatedAt(LocalDateTime.now());
        
        FinancialRecord updated = recordRepository.save(record);
        outboxService.enqueueFinancialRecordEvent(
                Constants.KAFKA_RECORD_UPDATED_TOPIC,
                buildRecordEvent(updated, "UPDATE", UUID.randomUUID().toString()));
        
        return updated;
    }

    @Transactional
    @CacheEvict(value = {"records", "record"}, allEntries = true)
    public void deleteRecord(String userId, String recordId) {
        FinancialRecord record = getRecordById(userId, recordId);
        record.setDeleted(true);
        record.setUpdatedAt(LocalDateTime.now());
        FinancialRecord deleted = recordRepository.save(record);
        outboxService.enqueueFinancialRecordEvent(
                Constants.KAFKA_RECORD_DELETED_TOPIC,
                buildRecordEvent(deleted, "DELETE", UUID.randomUUID().toString()));
    }

    public FinancialRecord createRecordFallback(String userId, BigDecimal amount, 
                                               String type, String category, String description, LocalDate date, Exception ex) {
        log.error("Circuit breaker fallback triggered: {}", ex.getMessage());
        throw new ServiceException("Record service temporarily unavailable", "SERVICE_UNAVAILABLE", 503);
    }

    private FinancialRecordEvent buildRecordEvent(FinancialRecord record, String action, String correlationId) {
        FinancialRecordEvent event = FinancialRecordEvent.builder()
                .recordId(record.getId())
                .amount(record.getAmount())
                .type(record.getType().name())
                .category(record.getCategory())
                .description(record.getDescription())
                .action(action)
                .build();
        event.setUserId(record.getUserId());
        event.setCorrelationId(correlationId);
        event.setSourceService("record-service");
        event.setEventType("FINANCIAL_RECORD_" + action);
        return event;
    }

    private FinancialRecord.RecordType parseRecordType(String type) {
        try {
            return FinancialRecord.RecordType.valueOf(type.trim().toUpperCase());
        } catch (RuntimeException ex) {
            throw new ServiceException("type must be INCOME or EXPENSE", "INVALID_RECORD_TYPE", 400);
        }
    }
}
