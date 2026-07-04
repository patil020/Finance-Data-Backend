package com.financeapp.event;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

/**
 * Domain events for Financial Record operations
 * Events are published to Kafka for:
 * - Dashboard cache invalidation
 * - Audit logging
 * - Analytics
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FinancialRecordEvent extends BaseEvent {
    @JsonProperty("record_id")
    private String recordId;
    
    @JsonProperty("amount")
    private BigDecimal amount;
    
    @JsonProperty("category")
    private String category;
    
    @JsonProperty("type")
    private String type; // INCOME or EXPENSE
    
    @JsonProperty("description")
    private String description;
    
    @JsonProperty("action")
    private String action; // CREATE, UPDATE, DELETE
}
