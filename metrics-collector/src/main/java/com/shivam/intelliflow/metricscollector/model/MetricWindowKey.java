package com.shivam.intelliflow.metricscollector.model;

import java.time.Instant;

public record MetricWindowKey(
        Instant windowStart,
        String serviceName
) {
}
