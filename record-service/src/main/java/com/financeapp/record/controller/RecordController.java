package com.financeapp.record.controller;

import com.financeapp.common.ApiResponse;
import com.financeapp.record.entity.FinancialRecord;
import com.financeapp.record.service.RecordService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DESIGN PATTERN: REST Controller Pattern
 * Exposes HTTP endpoints for record operations
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/api/records")
@RequiredArgsConstructor
public class RecordController {

    private final RecordService recordService;

    @PostMapping
    public ResponseEntity<ApiResponse<FinancialRecord>> createRecord(
            @Valid @RequestBody CreateRecordRequest request,
            @RequestHeader("X-User-Id") @NotBlank(message = "X-User-Id header is required") String userId) {
        
        FinancialRecord record = recordService.createRecord(
                userId, 
                request.getAmount(), 
                request.getType(), 
                request.getCategory(), 
                request.getDescription(),
                request.getDate()
        );
        return ResponseEntity.ok(ApiResponse.success(record, "Record created successfully"));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<FinancialRecord>>> getRecords(
            @RequestHeader("X-User-Id") @NotBlank(message = "X-User-Id header is required") String userId,
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "page must be zero or greater") int page,
            @RequestParam(defaultValue = "10") @Min(value = 1, message = "size must be at least 1") @Max(value = 200, message = "size must not exceed 200") int size) {
        
        Page<FinancialRecord> records = recordService.getRecordsByUser(userId, page, size);
        return ResponseEntity.ok(ApiResponse.success(records, "Records retrieved successfully"));
    }

    @GetMapping("/filter")
    public ResponseEntity<ApiResponse<Page<FinancialRecord>>> filterRecords(
            @RequestHeader("X-User-Id") @NotBlank(message = "X-User-Id header is required") String userId,
            @RequestParam(required = false) @Size(max = 100, message = "category must not exceed 100 characters") String category,
            @RequestParam(required = false) @Pattern(regexp = "INCOME|EXPENSE", flags = Pattern.Flag.CASE_INSENSITIVE, message = "type must be INCOME or EXPENSE") String type,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "page must be zero or greater") int page,
            @RequestParam(defaultValue = "10") @Min(value = 1, message = "size must be at least 1") @Max(value = 200, message = "size must not exceed 200") int size) {

        Page<FinancialRecord> records = recordService.filterRecords(
                userId,
                category,
                type,
                fromDate,
                toDate,
                page,
                size);
        return ResponseEntity.ok(ApiResponse.success(records, "Filtered records retrieved successfully"));
    }

    @GetMapping("/{recordId}")
    public ResponseEntity<ApiResponse<FinancialRecord>> getRecord(
            @PathVariable @NotBlank(message = "recordId is required") String recordId,
            @RequestHeader("X-User-Id") @NotBlank(message = "X-User-Id header is required") String userId) {
        
        FinancialRecord record = recordService.getRecordById(userId, recordId);
        return ResponseEntity.ok(ApiResponse.success(record, "Record retrieved successfully"));
    }

    @PutMapping("/{recordId}")
    public ResponseEntity<ApiResponse<FinancialRecord>> updateRecord(
            @PathVariable @NotBlank(message = "recordId is required") String recordId,
            @Valid @RequestBody UpdateRecordRequest request,
            @RequestHeader("X-User-Id") @NotBlank(message = "X-User-Id header is required") String userId) {
        
        FinancialRecord record = recordService.updateRecord(userId, recordId, request.getAmount(), request.getCategory());
        return ResponseEntity.ok(ApiResponse.success(record, "Record updated successfully"));
    }

    @DeleteMapping("/{recordId}")
    public ResponseEntity<ApiResponse<Void>> deleteRecord(
            @PathVariable @NotBlank(message = "recordId is required") String recordId,
            @RequestHeader("X-User-Id") @NotBlank(message = "X-User-Id header is required") String userId) {
        
        recordService.deleteRecord(userId, recordId);
        return ResponseEntity.ok(ApiResponse.success(null, "Record deleted successfully"));
    }
    @Data
    public static class CreateRecordRequest {
        @NotNull(message = "amount is required")
        @DecimalMin(value = "0.01", message = "amount must be greater than zero")
        private BigDecimal amount;

        @NotBlank(message = "type is required")
        @Pattern(regexp = "INCOME|EXPENSE", flags = Pattern.Flag.CASE_INSENSITIVE, message = "type must be INCOME or EXPENSE")
        private String type;

        @NotBlank(message = "category is required")
        @Size(max = 100, message = "category must not exceed 100 characters")
        private String category;

        @Size(max = 500, message = "description must not exceed 500 characters")
        private String description;

        @PastOrPresent(message = "date cannot be in the future")
        private LocalDate date;
    }

    @Data
    public static class UpdateRecordRequest {
        @NotNull(message = "amount is required")
        @DecimalMin(value = "0.01", message = "amount must be greater than zero")
        private BigDecimal amount;

        @NotBlank(message = "category is required")
        @Size(max = 100, message = "category must not exceed 100 characters")
        private String category;
    }
}
