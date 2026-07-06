package com.shivam.intelliflow.logaggregator.aggregation;

import java.time.Instant;

public record LogAggregateSnapshot(
        Instant windowStart,
        String serviceName,
        String level,
        long eventCount,
        long distinctTraceCount,
        int messageLengthMin,
        int messageLengthMax,
        long messageLengthTotal,
        double messageLengthAverage
) {
}
