package com.shivam.intelliflow.logaggregator.model;

import com.shivam.intelliflow.common.event.EventSchema;
import java.time.Instant;

public record LogEventEnvelope(
        EventSchema event,
        Instant receivedAt
) {
    public static LogEventEnvelope from(EventSchema event) {
        return new LogEventEnvelope(event, Instant.now());
    }
}
