package com.financeapp.record.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.financeapp.common.ServiceException;
import com.financeapp.event.FinancialRecordEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OutboxService {

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    public void enqueueFinancialRecordEvent(String topic, FinancialRecordEvent event) {
        try {
            OutboxEvent outboxEvent = new OutboxEvent();
            outboxEvent.setAggregateId(event.getRecordId());
            outboxEvent.setEventType(event.getEventType());
            outboxEvent.setTopic(topic);
            outboxEvent.setPayload(objectMapper.writeValueAsString(event));
            outboxEventRepository.save(outboxEvent);
        } catch (JsonProcessingException e) {
            throw new ServiceException("Failed to enqueue record event", e, "OUTBOX_ENQUEUE_FAILED", 500);
        }
    }
}
