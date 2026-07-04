package com.financeapp.event;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DESIGN PATTERN: Event Sourcing Pattern
 * Base event class for all domain events
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BaseEvent implements Serializable {
    @JsonProperty("event_id")
    private String eventId = UUID.randomUUID().toString();
    
    @JsonProperty("event_type")
    private String eventType;
    
    @JsonProperty("source_service")
    private String sourceService;
    
    @JsonProperty("timestamp")
    private LocalDateTime timestamp = LocalDateTime.now();
    
    @JsonProperty("correlation_id")
    private String correlationId;
    
    @JsonProperty("user_id")
    private String userId;
}
