package com.shivam.intelliflow.metricscollector.service;

import com.shivam.intelliflow.common.event.EventSchema;
import com.shivam.intelliflow.metricscollector.aggregation.MetricsAggregationService;
import com.shivam.intelliflow.metricscollector.model.MetricEvent;
import com.shivam.intelliflow.metricscollector.model.MetricEventParser;
import org.springframework.stereotype.Service;

@Service
public class MetricsIngestionService {
    private final MetricEventParser metricEventParser;
    private final MetricsAggregationService metricsAggregationService;

    public MetricsIngestionService(
            MetricEventParser metricEventParser,
            MetricsAggregationService metricsAggregationService
    ) {
        this.metricEventParser = metricEventParser;
        this.metricsAggregationService = metricsAggregationService;
    }

    public void ingest(EventSchema event) {
        MetricEvent metricEvent = metricEventParser.parse(event);
        metricsAggregationService.record(metricEvent);
    }
}
