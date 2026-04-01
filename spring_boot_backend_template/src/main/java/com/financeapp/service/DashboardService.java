package com.financeapp.service;

import com.financeapp.dto.response.CategorySummaryDto;
import com.financeapp.dto.response.DashboardSummaryDto;
import com.financeapp.dto.response.FinancialRecordResponseDto;
import com.financeapp.dto.response.MonthlyTrendDto;
import java.util.List;

public interface DashboardService {

    DashboardSummaryDto getSummary();

    List<CategorySummaryDto> getCategoryWiseTotals();

    List<FinancialRecordResponseDto> getRecentActivities(int limit);

    List<MonthlyTrendDto> getMonthlyTrends();
}
