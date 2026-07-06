package com.shivam.intelliflow.metricscollector.model;

import java.math.BigDecimal;
import java.time.Instant;

public record ServiceMetricSnapshot(
        Instant windowStart,
        String serviceName,
        long eventCount,
        long requestCount,
        long errorCount,
        BigDecimal throughputPerSecond,
        BigDecimal errorRatePercent,
        BigDecimal latencyP50Ms,
        BigDecimal latencyP95Ms,
        BigDecimal latencyP99Ms,
        long latencySampleCount
) {
}
