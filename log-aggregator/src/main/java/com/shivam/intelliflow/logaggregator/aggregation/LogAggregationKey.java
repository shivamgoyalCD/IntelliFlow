package com.shivam.intelliflow.logaggregator.aggregation;

import java.time.Instant;

record LogAggregationKey(
        Instant windowStart,
        String serviceName,
        String level
) {
}
