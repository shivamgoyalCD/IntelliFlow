package com.shivam.intelliflow.anomalydetector.detector;

import com.shivam.intelliflow.anomalydetector.model.AlertEvent;
import com.shivam.intelliflow.anomalydetector.model.MetricEvent;
import com.shivam.intelliflow.anomalydetector.model.AnomalyMetadata;
import com.shivam.intelliflow.anomalydetector.model.AnomalyType;
import com.shivam.intelliflow.anomalydetector.rules.AlertRule;
import com.shivam.intelliflow.anomalydetector.rules.AlertRuleType;
import com.shivam.intelliflow.common.event.EventLevel;
import com.shivam.intelliflow.common.util.TimestampUtils;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class ThresholdAnomalyDetector {
    private final DetectionStateStore detectionStateStore;

    public ThresholdAnomalyDetector(DetectionStateStore detectionStateStore) {
        this.detectionStateStore = detectionStateStore;
    }

    public Optional<AlertEvent> detect(MetricEvent metricEvent, AlertRule rule) {
        BigDecimal observedValue = observedValue(metricEvent, rule);
        if (observedValue == null || !breached(metricEvent, rule, observedValue)) {
            return Optional.empty();
        }

        Instant now = TimestampUtils.nowUtc();
        if (!detectionStateStore.canPublish(rule, now)) {
            return Optional.empty();
        }

        detectionStateStore.markPublished(rule, now);
        return Optional.of(alertEvent(metricEvent, rule, observedValue, now));
    }

    private BigDecimal observedValue(MetricEvent metricEvent, AlertRule rule) {
        return switch (rule.ruleType()) {
            case ERROR_RATE -> metricEvent.errorRatePercent();
            case LATENCY_P99 -> metricEvent.latencyP99Ms();
            case THROUGHPUT_DROP -> metricEvent.throughput();
            case CONSECUTIVE_ERRORS -> BigDecimal.valueOf(consecutiveErrors(metricEvent, rule));
        };
    }

    private boolean breached(MetricEvent metricEvent, AlertRule rule, BigDecimal observedValue) {
        return switch (rule.ruleType()) {
            case ERROR_RATE, LATENCY_P99 -> observedValue.compareTo(rule.thresholdValue()) >= 0;
            case THROUGHPUT_DROP -> observedValue.compareTo(rule.thresholdValue()) <= 0;
            case CONSECUTIVE_ERRORS -> observedValue.compareTo(BigDecimal.valueOf(consecutiveThreshold(rule))) >= 0;
        };
    }

    private int consecutiveErrors(MetricEvent metricEvent, AlertRule rule) {
        return detectionStateStore.recordErrorState(metricEvent.serviceName(), rule, isErrorMetric(metricEvent));
    }

    private int consecutiveThreshold(AlertRule rule) {
        if (rule.consecutiveCount() > 1) {
            return rule.consecutiveCount();
        }
        return Math.max(1, rule.thresholdValue().intValue());
    }

    private boolean isErrorMetric(MetricEvent metricEvent) {
        if (metricEvent.level() == EventLevel.ERROR || metricEvent.level() == EventLevel.FATAL) {
            return true;
        }
        if (metricEvent.errorCount() != null && metricEvent.errorCount() > 0) {
            return true;
        }
        String status = String.valueOf(metricEvent.metadata().getOrDefault("status", ""));
        if ("FAILED".equalsIgnoreCase(status) || "ERROR".equalsIgnoreCase(status)) {
            return true;
        }
        String metricName = metricEvent.metricName() == null ? "" : metricEvent.metricName().toLowerCase(java.util.Locale.ROOT);
        return !metricName.isBlank()
                && metricEvent.metricValue() != null
                && metricEvent.metricValue().compareTo(BigDecimal.ZERO) > 0
                && (metricName.contains("failed") || metricName.contains("error"));
    }

    private AlertEvent alertEvent(MetricEvent metricEvent, AlertRule rule, BigDecimal observedValue, Instant now) {
        return new AlertEvent(
                UUID.randomUUID(),
                now,
                metricEvent.serviceName(),
                rule.id(),
                rule.name(),
                rule.ruleType(),
                anomalyType(rule),
                rule.severity(),
                observedValue,
                thresholdFor(rule),
                metricEvent.traceId(),
                metricEvent.spanId(),
                message(metricEvent, rule, observedValue),
                AnomalyMetadata.threshold(metricName(metricEvent, rule), anomalyType(rule)),
                metadata(metricEvent, rule)
        );
    }

    private AnomalyType anomalyType(AlertRule rule) {
        return switch (rule.ruleType()) {
            case ERROR_RATE -> AnomalyType.THRESHOLD_ERROR_RATE;
            case LATENCY_P99 -> AnomalyType.THRESHOLD_LATENCY_P99;
            case THROUGHPUT_DROP -> AnomalyType.THRESHOLD_THROUGHPUT_DROP;
            case CONSECUTIVE_ERRORS -> AnomalyType.THRESHOLD_CONSECUTIVE_ERRORS;
        };
    }

    private String metricName(MetricEvent metricEvent, AlertRule rule) {
        if (metricEvent.metricName() != null && !metricEvent.metricName().isBlank()) {
            return metricEvent.metricName();
        }
        return switch (rule.ruleType()) {
            case ERROR_RATE -> "error_rate_percent";
            case LATENCY_P99 -> "latency_p99_ms";
            case THROUGHPUT_DROP -> "throughput";
            case CONSECUTIVE_ERRORS -> "consecutive_errors";
        };
    }

    private BigDecimal thresholdFor(AlertRule rule) {
        if (rule.ruleType() == AlertRuleType.CONSECUTIVE_ERRORS) {
            return BigDecimal.valueOf(consecutiveThreshold(rule));
        }
        return rule.thresholdValue();
    }

    private String message(MetricEvent metricEvent, AlertRule rule, BigDecimal observedValue) {
        return "Alert rule breached: " + rule.name()
                + " service=" + metricEvent.serviceName()
                + " observed=" + observedValue
                + " threshold=" + thresholdFor(rule);
    }

    private Map<String, Object> metadata(MetricEvent metricEvent, AlertRule rule) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("source_event_id", metricEvent.eventId());
        metadata.put("metric_name", metricName(metricEvent, rule));
        metadata.put("metric_value", metricEvent.metricValue());
        metadata.put("rule_type", rule.ruleType().name());
        metadata.put("evaluation_window_seconds", rule.evaluationWindowSeconds());
        metadata.put("source_metadata", metricEvent.metadata());
        return metadata;
    }
}
