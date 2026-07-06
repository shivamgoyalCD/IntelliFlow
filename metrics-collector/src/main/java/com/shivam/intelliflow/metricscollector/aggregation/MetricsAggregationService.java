package com.shivam.intelliflow.metricscollector.aggregation;

import com.shivam.intelliflow.metricscollector.cache.HotMetricsCache;
import com.shivam.intelliflow.metricscollector.model.MetricEvent;
import com.shivam.intelliflow.metricscollector.model.MetricWindowKey;
import com.shivam.intelliflow.metricscollector.model.ServiceMetricSnapshot;
import jakarta.annotation.PreDestroy;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class MetricsAggregationService {
    private static final Logger log = LoggerFactory.getLogger(MetricsAggregationService.class);

    private final Object monitor = new Object();
    private final Map<MetricWindowKey, ServiceMetricAccumulator> accumulators = new HashMap<>();
    private final MetricsAggregateStore metricsAggregateStore;
    private final HotMetricsCache hotMetricsCache;
    private final double tdigestCompression;

    public MetricsAggregationService(
            MetricsAggregateStore metricsAggregateStore,
            HotMetricsCache hotMetricsCache,
            @Value("${intelliflow.metrics-collector.aggregation.tdigest-compression:100}") double tdigestCompression
    ) {
        this.metricsAggregateStore = metricsAggregateStore;
        this.hotMetricsCache = hotMetricsCache;
        this.tdigestCompression = tdigestCompression;
    }

    public void record(MetricEvent event) {
        synchronized (monitor) {
            accumulators.computeIfAbsent(key(event), ignored -> new ServiceMetricAccumulator(tdigestCompression))
                    .record(event);
        }
    }

    @Scheduled(fixedRateString = "${intelliflow.metrics-collector.aggregation.flush-interval-ms:60000}")
    public void flush() {
        synchronized (monitor) {
            if (accumulators.isEmpty()) {
                return;
            }

            List<ServiceMetricSnapshot> snapshots = snapshots();
            try {
                metricsAggregateStore.saveBatch(snapshots);
                hotMetricsCache.cacheAll(snapshots);
                accumulators.clear();
            } catch (RuntimeException exception) {
                log.warn("Failed to flush {} service metric aggregate(s); retaining in memory", snapshots.size(), exception);
            }
        }
    }

    @PreDestroy
    public void flushOnShutdown() {
        flush();
    }

    private List<ServiceMetricSnapshot> snapshots() {
        List<ServiceMetricSnapshot> snapshots = new ArrayList<>(accumulators.size());
        for (Map.Entry<MetricWindowKey, ServiceMetricAccumulator> entry : accumulators.entrySet()) {
            snapshots.add(entry.getValue().snapshot(entry.getKey()));
        }
        return snapshots;
    }

    private MetricWindowKey key(MetricEvent event) {
        Instant windowStart = event.timestamp().truncatedTo(ChronoUnit.MINUTES);
        return new MetricWindowKey(windowStart, event.serviceName());
    }
}
