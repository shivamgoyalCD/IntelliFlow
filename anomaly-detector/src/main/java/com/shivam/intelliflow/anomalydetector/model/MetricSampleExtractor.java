package com.shivam.intelliflow.anomalydetector.model;

import com.shivam.intelliflow.common.util.TimestampUtils;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class MetricSampleExtractor {
    public List<MetricSample> extract(MetricEvent metricEvent) {
        Map<String, MetricSample> samples = new LinkedHashMap<>();
        Instant timestamp = metricEvent.timestamp() == null ? TimestampUtils.nowUtc() : metricEvent.timestamp();

        add(samples, metricEvent.serviceName(), metricEvent.metricName(), metricEvent.metricValue(), timestamp);
        add(samples, metricEvent.serviceName(), "error_rate_percent", metricEvent.errorRatePercent(), timestamp);
        add(samples, metricEvent.serviceName(), "latency_p99_ms", metricEvent.latencyP99Ms(), timestamp);
        add(samples, metricEvent.serviceName(), "throughput", metricEvent.throughput(), timestamp);

        return List.copyOf(samples.values());
    }

    private void add(
            Map<String, MetricSample> samples,
            String serviceName,
            String metricName,
            BigDecimal value,
            Instant timestamp
    ) {
        if (metricName == null || metricName.isBlank() || value == null) {
            return;
        }
        samples.putIfAbsent(metricName, new MetricSample(serviceName, metricName, value, timestamp));
    }
}
