package com.financeapp.dashboard.event;

import static org.mockito.Mockito.verify;

import com.financeapp.dashboard.service.DashboardCacheService;
import com.financeapp.event.FinancialRecordEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RecordEventListenerTest {

    @Mock
    private DashboardCacheService dashboardCacheService;

    private RecordEventListener listener;

    @BeforeEach
    void setUp() {
        listener = new RecordEventListener(dashboardCacheService);
    }

    @Test
    void recordCreatedInvalidatesDashboardCaches() {
        FinancialRecordEvent event = new FinancialRecordEvent();
        event.setRecordId("record-1");
        event.setUserId("user-1");

        listener.onRecordCreated(event);

        verify(dashboardCacheService).invalidateSummaryCache("user-1");
        verify(dashboardCacheService).invalidateCategoryWiseCache("user-1");
    }
}
