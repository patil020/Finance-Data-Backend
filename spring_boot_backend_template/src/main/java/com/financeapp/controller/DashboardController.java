package com.financeapp.controller;

import com.financeapp.dto.response.CategorySummaryDto;
import com.financeapp.dto.response.DashboardSummaryDto;
import com.financeapp.dto.response.FinancialRecordResponseDto;
import com.financeapp.dto.response.MonthlyTrendDto;
import com.financeapp.service.DashboardService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','ANALYST','VIEWER')")
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/summary")
    public ResponseEntity<DashboardSummaryDto> getSummary() {
        return ResponseEntity.ok(dashboardService.getSummary());
    }

    @GetMapping("/category-wise")
    public ResponseEntity<List<CategorySummaryDto>> getCategoryWise() {
        return ResponseEntity.ok(dashboardService.getCategoryWiseTotals());
    }

    @GetMapping("/recent-activity")
    public ResponseEntity<List<FinancialRecordResponseDto>> getRecentActivity(
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(dashboardService.getRecentActivities(limit));
    }

    @GetMapping("/monthly-trends")
    public ResponseEntity<List<MonthlyTrendDto>> getMonthlyTrends() {
        return ResponseEntity.ok(dashboardService.getMonthlyTrends());
    }
}
