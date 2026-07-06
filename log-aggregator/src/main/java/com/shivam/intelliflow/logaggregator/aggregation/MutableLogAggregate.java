package com.shivam.intelliflow.logaggregator.aggregation;

import com.shivam.intelliflow.common.event.EventSchema;
import com.shivam.intelliflow.logaggregator.model.LogEventEnvelope;
import java.util.HashSet;
import java.util.Set;

class MutableLogAggregate {
    private final Set<String> traceIds = new HashSet<>();

    private long eventCount;
    private long messageLengthTotal;
    private int messageLengthMin = Integer.MAX_VALUE;
    private int messageLengthMax;

    void record(LogEventEnvelope envelope) {
        EventSchema event = envelope.event();
        eventCount++;

        if (event.traceId() != null && !event.traceId().isBlank()) {
            traceIds.add(event.traceId());
        }

        int messageLength = event.message() == null ? 0 : event.message().length();
        messageLengthTotal += messageLength;
        messageLengthMin = Math.min(messageLengthMin, messageLength);
        messageLengthMax = Math.max(messageLengthMax, messageLength);
    }

    LogAggregateSnapshot snapshot(LogAggregationKey key) {
        return new LogAggregateSnapshot(
                key.windowStart(),
                key.serviceName(),
                key.level(),
                eventCount,
                traceIds.size(),
                messageLengthMin == Integer.MAX_VALUE ? 0 : messageLengthMin,
                messageLengthMax,
                messageLengthTotal,
                eventCount == 0 ? 0.0 : (double) messageLengthTotal / eventCount
        );
    }
}
