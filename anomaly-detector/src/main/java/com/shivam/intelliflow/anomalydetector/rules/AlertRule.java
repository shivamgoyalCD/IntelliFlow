package com.shivam.intelliflow.anomalydetector.rules;

import com.shivam.intelliflow.anomalydetector.model.AlertSeverity;
import java.math.BigDecimal;
import java.util.UUID;

public record AlertRule(
        UUID id,
        String name,
        String serviceName,
        AlertRuleType ruleType,
        BigDecimal thresholdValue,
        int evaluationWindowSeconds,
        int consecutiveCount,
        int cooldownSeconds,
        AlertSeverity severity,
        boolean enabled
) {
    public boolean appliesTo(String eventServiceName) {
        return "*".equals(serviceName()) || serviceName().equals(eventServiceName);
    }
}
