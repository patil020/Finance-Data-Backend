package com.financeapp.service.impl;

import com.financeapp.dao.FinancialRecordDao;
import com.financeapp.dao.UserDao;
import com.financeapp.dao.specification.FinancialRecordSpecification;
import com.financeapp.dto.request.FinancialRecordRequestDto;
import com.financeapp.dto.response.FinancialRecordResponseDto;
import com.financeapp.dto.response.PagedResponseDto;
import com.financeapp.entities.FinancialRecord;
import com.financeapp.entities.User;
import com.financeapp.enums.RecordType;
import com.financeapp.exception.BadRequestException;
import com.financeapp.exception.ResourceNotFoundException;
import com.financeapp.service.FinancialRecordService;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FinancialRecordServiceImpl implements FinancialRecordService {

    private final FinancialRecordDao financialRecordDao;
    private final UserDao userDao;
    private final ModelMapper modelMapper;

    @Override
    @Transactional
    public FinancialRecordResponseDto addRecord(FinancialRecordRequestDto requestDto, String loggedInEmail) {
        User user = userDao.findByEmailIgnoreCase(loggedInEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Logged in user not found."));

        FinancialRecord record = FinancialRecord.builder()
                .amount(requestDto.getAmount())
                .type(requestDto.getType())
                .category(requestDto.getCategory().trim())
                .recordDate(requestDto.getRecordDate())
                .note(requestDto.getNote())
                .createdBy(user)
                .deleted(false)
                .build();
        return mapRecordResponse(financialRecordDao.save(record));
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponseDto<FinancialRecordResponseDto> getAllRecords(int page, int size, String sortBy, String sortDir) {
        Pageable pageable = createPageable(page, size, sortBy, sortDir);
        Page<FinancialRecordResponseDto> responsePage = financialRecordDao.findAllByDeletedFalse(pageable).map(this::mapRecordResponse);
        return PagedResponseDto.fromPage(responsePage);
    }

    @Override
    @Transactional(readOnly = true)
    public FinancialRecordResponseDto getRecordById(Long id) {
        FinancialRecord record = financialRecordDao.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Record not found with id: " + id));
        return mapRecordResponse(record);
    }

    @Override
    @Transactional
    public FinancialRecordResponseDto updateRecord(Long id, FinancialRecordRequestDto requestDto) {
        FinancialRecord record = financialRecordDao.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Record not found with id: " + id));

        record.setAmount(requestDto.getAmount());
        record.setType(requestDto.getType());
        record.setCategory(requestDto.getCategory().trim());
        record.setRecordDate(requestDto.getRecordDate());
        record.setNote(requestDto.getNote());
        return mapRecordResponse(financialRecordDao.save(record));
    }

    @Override
    @Transactional
    public void deleteRecord(Long id) {
        FinancialRecord record = financialRecordDao.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Record not found with id: " + id));
        record.setDeleted(true);
        financialRecordDao.save(record);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponseDto<FinancialRecordResponseDto> filterRecords(
            RecordType type,
            String category,
            LocalDate startDate,
            LocalDate endDate,
            int page,
            int size,
            String sortBy,
            String sortDir) {

        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new BadRequestException("Start date cannot be after end date.");
        }

        Pageable pageable = createPageable(page, size, sortBy, sortDir);
        Specification<FinancialRecord> specification = FinancialRecordSpecification.notDeleted()
                .and(FinancialRecordSpecification.hasType(type))
                .and(FinancialRecordSpecification.hasCategory(category))
                .and(FinancialRecordSpecification.onOrAfter(startDate))
                .and(FinancialRecordSpecification.onOrBefore(endDate));

        Page<FinancialRecordResponseDto> responsePage = financialRecordDao.findAll(specification, pageable).map(this::mapRecordResponse);
        return PagedResponseDto.fromPage(responsePage);
    }

    private Pageable createPageable(int page, int size, String sortBy, String sortDir) {
        Sort.Direction direction = "desc".equalsIgnoreCase(sortDir) ? Sort.Direction.DESC : Sort.Direction.ASC;
        return PageRequest.of(page, size, Sort.by(direction, sortBy));
    }

    private FinancialRecordResponseDto mapRecordResponse(FinancialRecord record) {
        FinancialRecordResponseDto responseDto = modelMapper.map(record, FinancialRecordResponseDto.class);
        responseDto.setCreatedById(record.getCreatedBy().getId());
        responseDto.setCreatedByEmail(record.getCreatedBy().getEmail());
        return responseDto;
    }
}
