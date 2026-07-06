package com.shivam.intelliflow.anomalydetector.detector;

import com.shivam.intelliflow.anomalydetector.model.MetricStatistics;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class MetricStatisticsCalculator {
    public Optional<MetricStatistics> calculate(List<BigDecimal> baselineValues, BigDecimal observedValue) {
        if (baselineValues.isEmpty() || observedValue == null) {
            return Optional.empty();
        }

        double mean = baselineValues.stream()
                .mapToDouble(BigDecimal::doubleValue)
                .average()
                .orElse(0.0);
        double variance = baselineValues.stream()
                .mapToDouble(value -> Math.pow(value.doubleValue() - mean, 2))
                .average()
                .orElse(0.0);
        double standardDeviation = Math.sqrt(variance);

        if (standardDeviation == 0.0) {
            return Optional.empty();
        }

        double zScore = (observedValue.doubleValue() - mean) / standardDeviation;
        return Optional.of(MetricStatistics.from(mean, standardDeviation, zScore, baselineValues.size()));
    }
}
