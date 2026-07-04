package com.financeapp.dashboard.controller;

import com.financeapp.common.ApiResponse;
import com.financeapp.dashboard.service.DashboardCacheService;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * DESIGN PATTERN: REST Controller Pattern
 * Read-only API endpoints for dashboard queries
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardCacheService dashboardCacheService;

    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<DashboardCacheService.DashboardSummary>> getSummary(
            @RequestHeader("X-User-Id") @NotBlank(message = "X-User-Id header is required") String userId) {
        
        DashboardCacheService.DashboardSummary summary = dashboardCacheService.getSummary(userId);
        return ResponseEntity.ok(ApiResponse.success(summary, "Dashboard summary retrieved"));
    }
}
