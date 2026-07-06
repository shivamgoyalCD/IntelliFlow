package com.shivam.intelliflow.anomalydetector.detector;

import com.shivam.intelliflow.anomalydetector.model.AlertEvent;
import com.shivam.intelliflow.anomalydetector.model.AlertSeverity;
import com.shivam.intelliflow.anomalydetector.model.AnomalyMetadata;
import com.shivam.intelliflow.anomalydetector.model.AnomalyType;
import com.shivam.intelliflow.anomalydetector.model.MetricEvent;
import com.shivam.intelliflow.anomalydetector.model.MetricSample;
import com.shivam.intelliflow.anomalydetector.model.MetricSampleExtractor;
import com.shivam.intelliflow.anomalydetector.model.MetricStatistics;
import com.shivam.intelliflow.common.util.TimestampUtils;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class StatisticalAnomalyDetector {
    private static final Logger log = LoggerFactory.getLogger(StatisticalAnomalyDetector.class);

    private final MetricSampleExtractor metricSampleExtractor;
    private final MetricHistoryStore metricHistoryStore;
    private final MetricStatisticsCalculator statisticsCalculator;
    private final Duration historyWindow;
    private final int minSamples;
    private final BigDecimal zScoreThreshold;

    public StatisticalAnomalyDetector(
            MetricSampleExtractor metricSampleExtractor,
            MetricHistoryStore metricHistoryStore,
            MetricStatisticsCalculator statisticsCalculator,
            @Value("${intelliflow.anomaly-detector.statistical.history-window-seconds:900}") long historyWindowSeconds,
            @Value("${intelliflow.anomaly-detector.statistical.min-samples:20}") int minSamples,
            @Value("${intelliflow.anomaly-detector.statistical.z-score-threshold:3}") BigDecimal zScoreThreshold
    ) {
        this.metricSampleExtractor = metricSampleExtractor;
        this.metricHistoryStore = metricHistoryStore;
        this.statisticsCalculator = statisticsCalculator;
        this.historyWindow = Duration.ofSeconds(Math.max(60, historyWindowSeconds));
        this.minSamples = Math.max(2, minSamples);
        this.zScoreThreshold = zScoreThreshold.abs();
    }

    public List<AlertEvent> detect(MetricEvent metricEvent) {
        try {
            return metricSampleExtractor.extract(metricEvent).stream()
                    .flatMap(sample -> detect(metricEvent, sample).stream())
                    .toList();
        } catch (RuntimeException exception) {
            log.warn("Statistical anomaly detection failed for eventId={}", metricEvent.eventId(), exception);
            return List.of();
        }
    }

    private java.util.Optional<AlertEvent> detect(MetricEvent metricEvent, MetricSample sample) {
        List<BigDecimal> baselineValues = metricHistoryStore.recentValues(sample, historyWindow);
        metricHistoryStore.record(sample, historyWindow);

        if (baselineValues.size() < minSamples) {
            return java.util.Optional.empty();
        }

        return statisticsCalculator.calculate(baselineValues, sample.value())
                .filter(statistics -> statistics.zScore().abs().compareTo(zScoreThreshold) > 0)
                .map(statistics -> alertEvent(metricEvent, sample, statistics));
    }

    private AlertEvent alertEvent(MetricEvent metricEvent, MetricSample sample, MetricStatistics statistics) {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("history_window_seconds", historyWindow.toSeconds());
        details.put("source_event_id", metricEvent.eventId());
        details.put("source_metadata", metricEvent.metadata());

        AnomalyMetadata anomalyMetadata = new AnomalyMetadata(
                sample.metricName(),
                AnomalyType.Z_SCORE,
                statistics.mean(),
                statistics.standardDeviation(),
                statistics.zScore(),
                statistics.sampleCount(),
                details
        );

        return new AlertEvent(
                UUID.randomUUID(),
                TimestampUtils.nowUtc(),
                sample.serviceName(),
                null,
                null,
                null,
                AnomalyType.Z_SCORE,
                AlertSeverity.WARN,
                sample.value(),
                zScoreThreshold,
                metricEvent.traceId(),
                metricEvent.spanId(),
                message(sample, statistics),
                anomalyMetadata,
                Map.of("metric_value", sample.value())
        );
    }

    private String message(MetricSample sample, MetricStatistics statistics) {
        return "Statistical anomaly detected: service=" + sample.serviceName()
                + " metric=" + sample.metricName()
                + " z_score=" + statistics.zScore()
                + " threshold=" + zScoreThreshold;
    }
}
