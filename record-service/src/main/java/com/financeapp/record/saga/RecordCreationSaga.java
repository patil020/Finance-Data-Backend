package com.financeapp.record.saga;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DESIGN PATTERN: Saga Orchestration Pattern
 * Represents a saga state machine for distributed transactions
 * 
 * Example: Record Creation Saga
 * Step 1: Create record (local)
 * Step 2: Publish event to Dashboard service
 * Step 3: On failure, compensate by deleting the record
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecordCreationSaga {
    
    @JsonProperty("saga_id")
    private String sagaId;
    
    @JsonProperty("record_id")
    private String recordId;
    
    @JsonProperty("user_id")
    private String userId;
    
    @JsonProperty("status")
    private SagaStatus status = SagaStatus.PENDING;
    
    @JsonProperty("current_step")
    private int currentStep = 0;
    
    @JsonProperty("error_message")
    private String errorMessage;
    
    public enum SagaStatus {
        PENDING,      // Saga started
        APPROVED,     // All steps completed
        REJECTED,     // One step failed
        COMPENSATED   // Compensation steps executed
    }
}
