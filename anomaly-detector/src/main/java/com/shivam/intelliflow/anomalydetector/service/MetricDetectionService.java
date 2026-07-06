package com.shivam.intelliflow.anomalydetector.service;

import com.shivam.intelliflow.anomalydetector.detector.StatisticalAnomalyDetector;
import com.shivam.intelliflow.anomalydetector.detector.ThresholdAnomalyDetector;
import com.shivam.intelliflow.anomalydetector.model.AlertEvent;
import com.shivam.intelliflow.anomalydetector.model.MetricEvent;
import com.shivam.intelliflow.anomalydetector.model.MetricEventParser;
import com.shivam.intelliflow.anomalydetector.publisher.AlertEventPublisher;
import com.shivam.intelliflow.anomalydetector.rules.AlertRuleCache;
import com.shivam.intelliflow.common.event.EventSchema;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class MetricDetectionService {
    private final MetricEventParser metricEventParser;
    private final AlertRuleCache alertRuleCache;
    private final ThresholdAnomalyDetector thresholdAnomalyDetector;
    private final StatisticalAnomalyDetector statisticalAnomalyDetector;
    private final AlertDeduplicationService alertDeduplicationService;
    private final AlertEventPublisher alertEventPublisher;

    public MetricDetectionService(
            MetricEventParser metricEventParser,
            AlertRuleCache alertRuleCache,
            ThresholdAnomalyDetector thresholdAnomalyDetector,
            StatisticalAnomalyDetector statisticalAnomalyDetector,
            AlertDeduplicationService alertDeduplicationService,
            AlertEventPublisher alertEventPublisher
    ) {
        this.metricEventParser = metricEventParser;
        this.alertRuleCache = alertRuleCache;
        this.thresholdAnomalyDetector = thresholdAnomalyDetector;
        this.statisticalAnomalyDetector = statisticalAnomalyDetector;
        this.alertDeduplicationService = alertDeduplicationService;
        this.alertEventPublisher = alertEventPublisher;
    }

    public void evaluate(EventSchema event) {
        MetricEvent metricEvent = metricEventParser.parse(event);

        List<AlertEvent> alerts = new ArrayList<>();
        alerts.addAll(alertRuleCache.rulesFor(metricEvent.serviceName()).stream()
                .flatMap(rule -> thresholdAnomalyDetector.detect(metricEvent, rule).stream())
                .toList());
        alerts.addAll(statisticalAnomalyDetector.detect(metricEvent));

        alerts.stream()
                .filter(alertDeduplicationService::shouldPublish)
                .forEach(alertEventPublisher::publish);
    }
}
