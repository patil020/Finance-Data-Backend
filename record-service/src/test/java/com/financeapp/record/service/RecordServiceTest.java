package com.financeapp.record.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.financeapp.common.Constants;
import com.financeapp.common.ServiceException;
import com.financeapp.record.entity.FinancialRecord;
import com.financeapp.record.outbox.OutboxService;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RecordServiceTest {

    @Mock
    private FinancialRecordRepository recordRepository;

    @Mock
    private OutboxService outboxService;

    @InjectMocks
    private RecordService recordService;

    @Test
    void createRecordPersistsAndEnqueuesOutboxEvent() {
        when(recordRepository.save(any(FinancialRecord.class))).thenAnswer(invocation -> {
            FinancialRecord record = invocation.getArgument(0);
            record.setId("record-1");
            return record;
        });

        FinancialRecord record = recordService.createRecord(
                "user-1",
                BigDecimal.valueOf(2500),
                "income",
                "Salary",
                "July salary",
                LocalDate.of(2026, 7, 1));

        assertEquals("record-1", record.getId());
        assertEquals(FinancialRecord.RecordType.INCOME, record.getType());
        verify(outboxService).enqueueFinancialRecordEvent(
                eq(Constants.KAFKA_RECORD_CREATED_TOPIC),
                argThat(event ->
                        "record-1".equals(event.getRecordId())
                                && "user-1".equals(event.getUserId())
                                && "CREATE".equals(event.getAction())));
    }

    @Test
    void filterRecordsRejectsInvalidDateRange() {
        ServiceException exception = assertThrows(
                ServiceException.class,
                () -> recordService.filterRecords(
                        "user-1",
                        null,
                        null,
                        LocalDate.of(2026, 7, 2),
                        LocalDate.of(2026, 7, 1),
                        0,
                        10));

        assertEquals("INVALID_DATE_RANGE", exception.getErrorCode());
    }
}
