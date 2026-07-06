package com.shivam.intelliflow.anomalydetector.model;

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
        String traceId,
        String spanId,
        String metricName,
        BigDecimal metricValue,
        BigDecimal errorRatePercent,
        BigDecimal latencyP99Ms,
        BigDecimal throughput,
        Long errorCount,
        Long requestCount,
        Map<String, Object> metadata,
        EventSchema source
) {
}
