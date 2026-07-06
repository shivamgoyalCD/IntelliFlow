package com.shivam.intelliflow.anomalydetector.model;

import com.shivam.intelliflow.anomalydetector.rules.AlertRuleType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record AlertEvent(
        UUID alertId,
        Instant timestamp,
        String serviceName,
        UUID ruleId,
        String ruleName,
        AlertRuleType ruleType,
        AnomalyType anomalyType,
        AlertSeverity severity,
        BigDecimal observedValue,
        BigDecimal thresholdValue,
        String traceId,
        String spanId,
        String message,
        AnomalyMetadata anomalyMetadata,
        Map<String, Object> metadata
) {
}
