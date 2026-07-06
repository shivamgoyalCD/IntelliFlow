package com.shivam.intelliflow.logaggregator.aggregation;

import com.shivam.intelliflow.common.event.EventSchema;
import com.shivam.intelliflow.logaggregator.model.LogEventBatch;
import com.shivam.intelliflow.logaggregator.model.LogEventEnvelope;
import jakarta.annotation.PreDestroy;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class LogAggregationService {
    private static final Logger log = LoggerFactory.getLogger(LogAggregationService.class);
    private static final String UNKNOWN_SERVICE = "unknown";
    private static final String UNKNOWN_LEVEL = "UNKNOWN";

    private final Object monitor = new Object();
    private final Map<LogAggregationKey, MutableLogAggregate> windows = new HashMap<>();
    private final LogAggregateStore aggregateStore;

    public LogAggregationService(LogAggregateStore aggregateStore) {
        this.aggregateStore = aggregateStore;
    }

    public void recordBatch(LogEventBatch batch) {
        synchronized (monitor) {
            for (LogEventEnvelope envelope : batch.events()) {
                LogAggregationKey key = keyFor(envelope);
                windows.computeIfAbsent(key, ignored -> new MutableLogAggregate())
                        .record(envelope);
            }
        }
    }

    @Scheduled(fixedRateString = "${intelliflow.log-aggregator.aggregation.flush-interval-ms:60000}")
    public void flushAggregates() {
        synchronized (monitor) {
            if (windows.isEmpty()) {
                return;
            }

            List<LogAggregateSnapshot> snapshots = snapshots();
            try {
                aggregateStore.saveBatch(snapshots);
                windows.clear();
            } catch (RuntimeException exception) {
                log.warn(
                        "Failed to flush {} log aggregate window(s); retaining in-memory aggregates for retry",
                        snapshots.size(),
                        exception
                );
            }
        }
    }

    @PreDestroy
    public void flushOnShutdown() {
        flushAggregates();
    }

    private List<LogAggregateSnapshot> snapshots() {
        List<LogAggregateSnapshot> snapshots = new ArrayList<>(windows.size());
        for (Map.Entry<LogAggregationKey, MutableLogAggregate> entry : windows.entrySet()) {
            snapshots.add(entry.getValue().snapshot(entry.getKey()));
        }
        return snapshots;
    }

    private LogAggregationKey keyFor(LogEventEnvelope envelope) {
        EventSchema event = envelope.event();
        Instant eventTime = event.timestamp() == null ? envelope.receivedAt() : event.timestamp();
        return new LogAggregationKey(
                eventTime.truncatedTo(ChronoUnit.MINUTES),
                normalize(event.serviceName(), UNKNOWN_SERVICE),
                event.level() == null ? UNKNOWN_LEVEL : event.level().name()
        );
    }

    private String normalize(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
