package com.financeapp.record.service;

import com.financeapp.record.saga.RecordCreationSaga;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * DESIGN PATTERN: Saga Orchestrator Pattern
 * Handles compensation logic when saga steps fail
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SagaOrchestrator {

    private final FinancialRecordRepository recordRepository;

    /**
     * DESIGN PATTERN: Compensation Transaction Pattern
     * Rolls back changes when a saga step fails
     */
    public void compensate(RecordCreationSaga saga) {
        log.info("Starting compensation for saga: {}", saga.getSagaId());
        
        // Compensation based on which step failed
        switch (saga.getCurrentStep()) {
            case 3:
                // Downstream step failed after event publish attempt - delete record
                compensateRecordCreation(saga);
                break;
            case 2:
                // Record was created, but event publishing did not complete
                compensateRecordCreation(saga);
                break;
            default:
                log.warn("Unknown step for compensation: {}", saga.getCurrentStep());
        }
    }

    private void compensateRecordCreation(RecordCreationSaga saga) {
        try {
            recordRepository.deleteById(saga.getRecordId());
            log.info("Compensation: Record deleted - {}", saga.getRecordId());
        } catch (Exception e) {
            log.error("Compensation failed: {}", e.getMessage());
        }
    }
}
