package com.financeapp.record.event;

import com.financeapp.common.Constants;
import com.financeapp.event.FinancialRecordEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import java.util.concurrent.TimeUnit;

/**
 * DESIGN PATTERN: Publisher/Event Emitter Pattern
 * Publishes domain events to Kafka topics
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RecordEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${app.kafka.publish-timeout-seconds:5}")
    private long publishTimeoutSeconds;

    public void publishRecordCreated(FinancialRecordEvent event) {
        publish(Constants.KAFKA_RECORD_CREATED_TOPIC, event.getRecordId(), event);
    }

    public void publishRecordUpdated(FinancialRecordEvent event) {
        publish(Constants.KAFKA_RECORD_UPDATED_TOPIC, event.getRecordId(), event);
    }

    public void publishRecordDeleted(FinancialRecordEvent event) {
        publish(Constants.KAFKA_RECORD_DELETED_TOPIC, event.getRecordId(), event);
    }

    public void publish(String topic, String key, Object event) {
        try {
            kafkaTemplate.send(topic, key, event)
                    .get(publishTimeoutSeconds, TimeUnit.SECONDS);
            log.info("Event published to topic {} with key {}", topic, key);
        } catch (Exception e) {
            log.error("Failed to publish event to topic {}: {}", topic, e.getMessage());
            throw new IllegalStateException("Failed to publish event to topic " + topic, e);
        }
    }
}
