package com.shivam.intelliflow.anomalydetector.model;

import com.shivam.intelliflow.common.event.EventSchema;
import java.math.BigDecimal;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class MetricEventParser {
    public MetricEvent parse(EventSchema event) {
        Map<String, Object> metadata = event.metadata();
        String metricName = stringValue(metadata.get("metric_name"));
        BigDecimal metricValue = decimalValue(metadata.get("metric_value"));

        return new MetricEvent(
                event.eventId(),
                event.timestamp(),
                serviceName(event, metadata),
                event.level(),
                event.traceId(),
                event.spanId(),
                metricName,
                metricValue,
                errorRate(metadata, metricName, metricValue),
                latencyP99(metadata, metricName, metricValue),
                throughput(metadata, metricName, metricValue),
                longValue(firstPresent(metadata, "error_count", "errors", "failed_count")),
                longValue(firstPresent(metadata, "request_count", "requests", "total_count")),
                metadata,
                event
        );
    }

    private String serviceName(EventSchema event, Map<String, Object> metadata) {
        String serviceName = stringValue(firstPresent(metadata, "target_service", "service_name", "service"));
        if (serviceName != null && !serviceName.isBlank()) {
            return serviceName;
        }
        return event.serviceName();
    }

    private BigDecimal errorRate(Map<String, Object> metadata, String metricName, BigDecimal metricValue) {
        BigDecimal explicit = decimalValue(firstPresent(metadata, "error_rate_percent", "error_rate", "failure_rate"));
        if (explicit != null) {
            return explicit;
        }

        Long errors = longValue(firstPresent(metadata, "error_count", "errors", "failed_count"));
        Long requests = longValue(firstPresent(metadata, "request_count", "requests", "total_count"));
        if (errors != null && requests != null && requests > 0) {
            return BigDecimal.valueOf(errors)
                    .multiply(BigDecimal.valueOf(100))
                    .divide(BigDecimal.valueOf(requests), 4, java.math.RoundingMode.HALF_UP);
        }

        if (metricName != null && metricName.toLowerCase(Locale.ROOT).contains("error.rate")) {
            return metricValue;
        }

        return null;
    }

    private BigDecimal latencyP99(Map<String, Object> metadata, String metricName, BigDecimal metricValue) {
        BigDecimal explicit = decimalValue(firstPresent(metadata, "latency_p99_ms", "p99_latency_ms", "p99_ms"));
        if (explicit != null) {
            return explicit;
        }

        if (metricName != null) {
            String normalized = metricName.toLowerCase(Locale.ROOT);
            if (normalized.contains("latency.p99") || normalized.contains("p99.latency")) {
                return metricValue;
            }
        }

        return null;
    }

    private BigDecimal throughput(Map<String, Object> metadata, String metricName, BigDecimal metricValue) {
        BigDecimal explicit = decimalValue(firstPresent(
                metadata,
                "throughput",
                "throughput_per_minute",
                "requests_per_minute",
                "events_per_minute"
        ));
        if (explicit != null) {
            return explicit;
        }

        if (metricName != null && metricName.toLowerCase(Locale.ROOT).contains("throughput")) {
            return metricValue;
        }

        return null;
    }

    private Object firstPresent(Map<String, Object> metadata, String... keys) {
        for (String key : keys) {
            if (metadata.containsKey(key)) {
                return metadata.get(key);
            }
        }
        return null;
    }

    private String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private BigDecimal decimalValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof BigDecimal decimal) {
            return decimal;
        }
        if (value instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue());
        }
        try {
            return new BigDecimal(String.valueOf(value));
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private Long longValue(Object value) {
        BigDecimal decimal = decimalValue(value);
        return decimal == null ? null : decimal.longValue();
    }
}
