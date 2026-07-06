package com.shivam.intelliflow.anomalydetector.model;

import java.util.Locale;

public record AlertDeduplicationKey(
        String serviceName,
        String metricName,
        AnomalyType anomalyType
) {
    public static AlertDeduplicationKey from(AlertEvent alertEvent) {
        String metricName = alertEvent.anomalyMetadata() == null
                ? "unknown"
                : alertEvent.anomalyMetadata().metricName();
        return new AlertDeduplicationKey(alertEvent.serviceName(), metricName, alertEvent.anomalyType());
    }

    public String redisKey(String prefix) {
        return prefix + ":" + normalize(serviceName) + ":" + normalize(metricName) + ":" + anomalyType.name();
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return "unknown";
        }
        return value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_.-]", "_");
    }
}
