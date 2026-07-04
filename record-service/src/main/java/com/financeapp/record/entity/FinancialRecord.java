package com.financeapp.record.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * DESIGN PATTERN: Entity Pattern (JPA)
 * Database entity for Financial Records
 */
@Entity
@Table(
        name = "financial_records",
        indexes = {
                @Index(name = "idx_financial_records_user_deleted", columnList = "user_id,deleted"),
                @Index(name = "idx_financial_records_user_date", columnList = "user_id,date"),
                @Index(name = "idx_financial_records_user_type", columnList = "user_id,type"),
                @Index(name = "idx_financial_records_user_category", columnList = "user_id,category")
        })
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FinancialRecord {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    
    @JsonProperty("user_id")
    @Column(name = "user_id", nullable = false)
    private String userId;
    
    @JsonProperty("amount")
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;
    
    @JsonProperty("type")
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private RecordType type;
    
    @JsonProperty("category")
    @Column(nullable = false)
    private String category;
    
    @JsonProperty("description")
    private String description;
    
    @JsonProperty("date")
    @Column(nullable = false)
    private LocalDate date;
    
    @JsonProperty("deleted")
    @Column(nullable = false)
    private boolean deleted = false;
    
    @JsonProperty("created_at")
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @JsonProperty("updated_at")
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    public enum RecordType {
        INCOME, EXPENSE
    }
}
