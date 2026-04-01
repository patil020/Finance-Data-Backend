package com.financeapp.service;

import com.financeapp.dto.request.FinancialRecordRequestDto;
import com.financeapp.dto.response.FinancialRecordResponseDto;
import com.financeapp.dto.response.PagedResponseDto;
import com.financeapp.enums.RecordType;
import java.time.LocalDate;

public interface FinancialRecordService {

    FinancialRecordResponseDto addRecord(FinancialRecordRequestDto requestDto, String loggedInEmail);

    PagedResponseDto<FinancialRecordResponseDto> getAllRecords(int page, int size, String sortBy, String sortDir);

    FinancialRecordResponseDto getRecordById(Long id);

    FinancialRecordResponseDto updateRecord(Long id, FinancialRecordRequestDto requestDto);

    void deleteRecord(Long id);

    PagedResponseDto<FinancialRecordResponseDto> filterRecords(
            RecordType type,
            String category,
            LocalDate startDate,
            LocalDate endDate,
            int page,
            int size,
            String sortBy,
            String sortDir);
}
