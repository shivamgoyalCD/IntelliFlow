package com.shivam.intelliflow.logaggregator.service;

import com.shivam.intelliflow.common.event.EventSchema;
import com.shivam.intelliflow.logaggregator.aggregation.LogAggregationService;
import com.shivam.intelliflow.logaggregator.elastic.LogEventIndexer;
import com.shivam.intelliflow.logaggregator.model.LogEventBatch;
import com.shivam.intelliflow.logaggregator.model.LogEventEnvelope;
import com.shivam.intelliflow.logaggregator.persistence.LogEventBatchFailureHandler;
import com.shivam.intelliflow.logaggregator.persistence.LogEventPersistenceRetryExecutor;
import com.shivam.intelliflow.logaggregator.persistence.LogEventStore;
import jakarta.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class LogIngestionService {
    private final Object monitor = new Object();
    private final List<PendingLogEvent> pendingEvents = new ArrayList<>();

    private final LogEventStore logEventStore;
    private final LogAggregationService logAggregationService;
    private final LogEventIndexer logEventIndexer;
    private final LogEventPersistenceRetryExecutor retryExecutor;
    private final LogEventBatchFailureHandler failureHandler;
    private final int batchSize;

    private boolean flushInProgress;

    public LogIngestionService(
            LogEventStore logEventStore,
            LogAggregationService logAggregationService,
            LogEventIndexer logEventIndexer,
            LogEventPersistenceRetryExecutor retryExecutor,
            LogEventBatchFailureHandler failureHandler,
            @Value("${intelliflow.log-aggregator.persistence.batch-size:100}") int batchSize
    ) {
        this.logEventStore = logEventStore;
        this.logAggregationService = logAggregationService;
        this.logEventIndexer = logEventIndexer;
        this.retryExecutor = retryExecutor;
        this.failureHandler = failureHandler;
        this.batchSize = Math.max(1, batchSize);
    }

    public void ingest(EventSchema event, Acknowledgment acknowledgment) {
        LogEventEnvelope envelope = LogEventEnvelope.from(event);
        boolean shouldFlush;

        synchronized (monitor) {
            pendingEvents.add(new PendingLogEvent(envelope, acknowledgment));
            shouldFlush = pendingEvents.size() >= batchSize;
        }

        if (shouldFlush) {
            flushPending(true);
        }
    }

    @Scheduled(fixedDelayString = "${intelliflow.log-aggregator.persistence.flush-interval-ms:500}")
    public void flushOnInterval() {
        flushPending(false);
    }

    @PreDestroy
    public void flushOnShutdown() {
        flushPending(false);
    }

    private void flushPending(boolean propagateFailure) {
        List<PendingLogEvent> batch = pendingBatch();
        if (batch.isEmpty()) {
            return;
        }

        LogEventBatch eventBatch = new LogEventBatch(batch.stream()
                .map(PendingLogEvent::envelope)
                .toList());

        try {
            retryExecutor.execute(() -> logEventStore.saveBatch(eventBatch));
            logAggregationService.recordBatch(eventBatch);
            logEventIndexer.indexBatch(eventBatch);
            markPersisted(batch);
        } catch (RuntimeException exception) {
            failureHandler.handleFailure(eventBatch, exception);
            markFlushComplete();

            if (propagateFailure) {
                throw exception;
            }
        }
    }

    private List<PendingLogEvent> pendingBatch() {
        synchronized (monitor) {
            if (flushInProgress || pendingEvents.isEmpty()) {
                return List.of();
            }

            flushInProgress = true;
            int batchLimit = Math.min(batchSize, pendingEvents.size());
            return List.copyOf(pendingEvents.subList(0, batchLimit));
        }
    }

    private void markPersisted(List<PendingLogEvent> batch) {
        synchronized (monitor) {
            pendingEvents.removeAll(batch);
            flushInProgress = false;
        }

        batch.forEach(PendingLogEvent::acknowledge);
    }

    private void markFlushComplete() {
        synchronized (monitor) {
            flushInProgress = false;
        }
    }

    private record PendingLogEvent(LogEventEnvelope envelope, Acknowledgment acknowledgment) {
        private void acknowledge() {
            acknowledgment.acknowledge();
        }
    }
}
