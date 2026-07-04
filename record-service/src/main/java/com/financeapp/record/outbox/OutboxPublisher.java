package com.financeapp.record.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.financeapp.common.Constants;
import com.financeapp.event.FinancialRecordEvent;
import com.financeapp.record.event.RecordEventPublisher;
import com.financeapp.record.outbox.OutboxEvent.OutboxStatus;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class OutboxPublisher {

    private final OutboxEventRepository outboxEventRepository;
    private final RecordEventPublisher recordEventPublisher;
    private final ObjectMapper objectMapper;

    @Value("${app.outbox.max-attempts:5}")
    private int maxAttempts;

    @Scheduled(
            initialDelayString = "${app.outbox.publish-initial-delay-ms:5000}",
            fixedDelayString = "${app.outbox.publish-fixed-delay-ms:5000}")
    @Transactional
    public void publishPendingEvents() {
        List<OutboxEvent> events = outboxEventRepository.findTop50ByStatusInOrderByCreatedAtAsc(
                List.of(OutboxStatus.PENDING, OutboxStatus.FAILED));

        for (OutboxEvent event : events) {
            publishOne(event);
        }
    }

    private void publishOne(OutboxEvent event) {
        if (event.getAttempts() >= maxAttempts) {
            sendToDeadLetter(event, "Maximum publish attempts exceeded");
            return;
        }

        try {
            FinancialRecordEvent payload = objectMapper.readValue(event.getPayload(), FinancialRecordEvent.class);
            recordEventPublisher.publish(event.getTopic(), payload.getRecordId(), payload);
            event.setStatus(OutboxStatus.PUBLISHED);
            event.setPublishedAt(LocalDateTime.now());
            event.setLastError(null);
            log.info("Outbox event published: {}", event.getId());
        } catch (Exception ex) {
            event.setAttempts(event.getAttempts() + 1);
            event.setStatus(event.getAttempts() >= maxAttempts ? OutboxStatus.DEAD_LETTER : OutboxStatus.FAILED);
            event.setLastError(limit(ex.getMessage()));
            log.error("Outbox event publish failed: {}", event.getId(), ex);
            if (event.getStatus() == OutboxStatus.DEAD_LETTER) {
                sendToDeadLetter(event, ex.getMessage());
            }
        }
    }

    private void sendToDeadLetter(OutboxEvent event, String reason) {
        try {
            recordEventPublisher.publish(Constants.KAFKA_RECORD_DEAD_LETTER_TOPIC, event.getAggregateId(), event);
        } catch (Exception ex) {
            log.error("Failed to publish outbox event to dead-letter topic: {}", event.getId(), ex);
        }
        event.setStatus(OutboxStatus.DEAD_LETTER);
        event.setLastError(limit(reason));
    }

    private String limit(String value) {
        if (value == null) {
            return null;
        }
        return value.length() <= 1000 ? value : value.substring(0, 1000);
    }
}
