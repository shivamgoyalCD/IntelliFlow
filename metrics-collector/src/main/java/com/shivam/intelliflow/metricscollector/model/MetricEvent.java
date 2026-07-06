package com.shivam.intelliflow.metricscollector.model;

import com.shivam.intelliflow.common.event.EventLevel;
import com.shivam.intelliflow.common.event.EventSchema;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

public record MetricEvent(
        String eventId,
        Instant timestamp,
        String serviceName,
        EventLevel level,
        String metricName,
        BigDecimal metricValue,
        BigDecimal latencyMs,
        long requestCount,
        long errorCount,
        Map<String, Object> metadata,
        EventSchema source
) {
}
