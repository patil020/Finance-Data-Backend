package com.financeapp.service.impl;

import com.financeapp.dao.FinancialRecordDao;
import com.financeapp.dto.response.CategorySummaryDto;
import com.financeapp.dto.response.DashboardSummaryDto;
import com.financeapp.dto.response.FinancialRecordResponseDto;
import com.financeapp.dto.response.MonthlyTrendDto;
import com.financeapp.entities.FinancialRecord;
import com.financeapp.enums.RecordType;
import com.financeapp.service.DashboardService;
import java.math.BigDecimal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final FinancialRecordDao financialRecordDao;
    private final ModelMapper modelMapper;

    @Override
    @Transactional(readOnly = true)
    public DashboardSummaryDto getSummary() {
        BigDecimal totalIncome = financialRecordDao.sumByType(RecordType.INCOME);
        BigDecimal totalExpense = financialRecordDao.sumByType(RecordType.EXPENSE);
        BigDecimal netBalance = totalIncome.subtract(totalExpense);

        return DashboardSummaryDto.builder()
                .totalIncome(totalIncome)
                .totalExpense(totalExpense)
                .netBalance(netBalance)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategorySummaryDto> getCategoryWiseTotals() {
        return financialRecordDao.fetchCategoryWiseTotals().stream()
                .map(row -> CategorySummaryDto.builder()
                        .category(String.valueOf(row[0]))
                        .total(toBigDecimal(row[1]))
                        .build())
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<FinancialRecordResponseDto> getRecentActivities(int limit) {
        int pageSize = Math.max(1, Math.min(limit, 50));
        return financialRecordDao.findByDeletedFalseOrderByCreatedAtDesc(PageRequest.of(0, pageSize))
                .stream()
                .map(this::mapRecordResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<MonthlyTrendDto> getMonthlyTrends() {
        return financialRecordDao.fetchMonthlyTrends().stream()
                .map(row -> {
                    BigDecimal income = toBigDecimal(row[2]);
                    BigDecimal expense = toBigDecimal(row[3]);
                    return MonthlyTrendDto.builder()
                            .year(((Number) row[0]).intValue())
                            .month(((Number) row[1]).intValue())
                            .totalIncome(income)
                            .totalExpense(expense)
                            .netBalance(income.subtract(expense))
                            .build();
                })
                .toList();
    }

    private FinancialRecordResponseDto mapRecordResponse(FinancialRecord record) {
        FinancialRecordResponseDto responseDto = modelMapper.map(record, FinancialRecordResponseDto.class);
        responseDto.setCreatedById(record.getCreatedBy().getId());
        responseDto.setCreatedByEmail(record.getCreatedBy().getEmail());
        return responseDto;
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        if (value instanceof BigDecimal bigDecimal) {
            return bigDecimal;
        }
        return new BigDecimal(value.toString());
    }
}
