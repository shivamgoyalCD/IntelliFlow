package com.shivam.intelliflow.anomalydetector.model;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public record AnomalyMetadata(
        String metricName,
        AnomalyType anomalyType,
        BigDecimal baselineMean,
        BigDecimal baselineStdDev,
        BigDecimal zScore,
        Integer sampleCount,
        Map<String, Object> details
) {
    public AnomalyMetadata {
        details = details == null
                ? Collections.emptyMap()
                : Collections.unmodifiableMap(new LinkedHashMap<>(details));
    }

    public static AnomalyMetadata threshold(String metricName, AnomalyType anomalyType) {
        return new AnomalyMetadata(metricName, anomalyType, null, null, null, null, Map.of());
    }
}
