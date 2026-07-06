package com.shivam.intelliflow.anomalydetector.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

public record MetricStatistics(
        BigDecimal mean,
        BigDecimal standardDeviation,
        BigDecimal zScore,
        int sampleCount
) {
    public static MetricStatistics from(double mean, double standardDeviation, double zScore, int sampleCount) {
        return new MetricStatistics(
                decimal(mean),
                decimal(standardDeviation),
                decimal(zScore),
                sampleCount
        );
    }

    private static BigDecimal decimal(double value) {
        return BigDecimal.valueOf(value).setScale(4, RoundingMode.HALF_UP);
    }
}
