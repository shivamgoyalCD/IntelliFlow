package com.shivam.intelliflow.logaggregator.model;

import java.util.List;

public record LogEventBatch(List<LogEventEnvelope> events) {
    public LogEventBatch {
        events = List.copyOf(events);
    }

    public int size() {
        return events.size();
    }

    public boolean isEmpty() {
        return events.isEmpty();
    }
}
