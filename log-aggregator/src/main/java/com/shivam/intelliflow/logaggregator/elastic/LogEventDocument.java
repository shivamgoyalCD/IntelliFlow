package com.shivam.intelliflow.logaggregator.elastic;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.shivam.intelliflow.common.event.EventSchema;
import com.shivam.intelliflow.logaggregator.model.LogEventEnvelope;
import java.time.Instant;
import java.util.Map;

public record LogEventDocument(
        String eventId,
        @JsonProperty("@timestamp")
        Instant timestamp,
        String serviceName,
        String level,
        String traceId,
        String spanId,
        String message,
        Map<String, Object> metadata,
        EventSchema rawEvent,
        Instant receivedAt
) {
    public static LogEventDocument from(LogEventEnvelope envelope) {
        EventSchema event = envelope.event();

        return new LogEventDocument(
                event.eventId(),
                event.timestamp(),
                event.serviceName(),
                event.level().name(),
                event.traceId(),
                event.spanId(),
                event.message(),
                event.metadata(),
                event,
                envelope.receivedAt()
        );
    }
}
