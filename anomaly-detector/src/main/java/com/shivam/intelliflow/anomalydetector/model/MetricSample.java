package com.shivam.intelliflow.anomalydetector.model;

import java.math.BigDecimal;
import java.time.Instant;

public record MetricSample(
        String serviceName,
        String metricName,
        BigDecimal value,
        Instant timestamp
) {
}
