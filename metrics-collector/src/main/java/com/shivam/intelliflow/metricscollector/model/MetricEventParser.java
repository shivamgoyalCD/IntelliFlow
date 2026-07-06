package com.shivam.intelliflow.metricscollector.model;

import com.shivam.intelliflow.common.event.EventLevel;
import com.shivam.intelliflow.common.event.EventSchema;
import com.shivam.intelliflow.common.util.TimestampUtils;
import java.math.BigDecimal;
import java.time.Instant;
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
                timestamp(event),
                serviceName(event, metadata),
                event.level(),
                metricName,
                metricValue,
                latency(metadata, metricName, metricValue),
                requestCount(metadata, metricName, metricValue),
                errorCount(event, metadata, metricName, metricValue),
                metadata,
                event
        );
    }

    private Instant timestamp(EventSchema event) {
        return event.timestamp() == null ? TimestampUtils.nowUtc() : event.timestamp();
    }

    private String serviceName(EventSchema event, Map<String, Object> metadata) {
        String serviceName = stringValue(firstPresent(metadata, "target_service", "service_name", "service"));
        if (serviceName != null && !serviceName.isBlank()) {
            return serviceName;
        }
        return event.serviceName() == null || event.serviceName().isBlank() ? "unknown" : event.serviceName();
    }

    private BigDecimal latency(Map<String, Object> metadata, String metricName, BigDecimal metricValue) {
        BigDecimal explicit = decimalValue(firstPresent(
                metadata,
                "latency_ms",
                "duration_ms",
                "request_latency_ms",
                "response_time_ms"
        ));
        if (explicit != null) {
            return explicit;
        }

        if (metricName == null || metricValue == null) {
            return null;
        }

        String normalized = metricName.toLowerCase(Locale.ROOT);
        if (normalized.endsWith(".latency") || normalized.endsWith(".latency_ms") || normalized.equals("latency_ms")) {
            return metricValue;
        }

        return null;
    }

    private long requestCount(Map<String, Object> metadata, String metricName, BigDecimal metricValue) {
        Long explicit = longValue(firstPresent(metadata, "request_count", "requests", "total_count"));
        if (explicit != null) {
            return Math.max(0, explicit);
        }

        if (metricName != null && metricValue != null && isCountMetric(metricName)) {
            return Math.max(0, metricValue.longValue());
        }

        return 1;
    }

    private long errorCount(EventSchema event, Map<String, Object> metadata, String metricName, BigDecimal metricValue) {
        Long explicit = longValue(firstPresent(metadata, "error_count", "errors", "failed_count"));
        if (explicit != null) {
            return Math.max(0, explicit);
        }

        String status = stringValue(metadata.get("status"));
        if ("FAILED".equalsIgnoreCase(status) || "ERROR".equalsIgnoreCase(status)) {
            return 1;
        }

        if (event.level() == EventLevel.ERROR || event.level() == EventLevel.FATAL) {
            return 1;
        }

        if (metricName != null && metricValue != null) {
            String normalized = metricName.toLowerCase(Locale.ROOT);
            if ((normalized.contains("error") || normalized.contains("failed")) && metricValue.signum() > 0) {
                return Math.max(1, metricValue.longValue());
            }
        }

        return 0;
    }

    private boolean isCountMetric(String metricName) {
        String normalized = metricName.toLowerCase(Locale.ROOT);
        return normalized.contains("request")
                || normalized.contains("created")
                || normalized.contains("processed")
                || normalized.contains("success")
                || normalized.contains("failed")
                || normalized.contains("error")
                || normalized.contains("throughput");
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
