package com.shivam.intelliflow.logaggregator.consumer;

import com.shivam.intelliflow.common.constants.KafkaTopicConstants;
import com.shivam.intelliflow.common.event.EventSchema;
import com.shivam.intelliflow.logaggregator.service.LogEventProcessingException;
import com.shivam.intelliflow.logaggregator.service.LogIngestionService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
public class LogEventConsumer {
    private static final String FORCE_DLQ_METADATA_KEY = "force_dlq";

    private final LogIngestionService logIngestionService;

    public LogEventConsumer(LogIngestionService logIngestionService) {
        this.logIngestionService = logIngestionService;
    }

    @KafkaListener(
            id = "log-aggregator-logs-consumer",
            topics = KafkaTopicConstants.LOGS,
            containerFactory = "logAggregatorKafkaListenerContainerFactory",
            autoStartup = "${intelliflow.log-aggregator.consumer.enabled:true}"
    )
    public void consume(EventSchema event, Acknowledgment acknowledgment) {
        rejectPoisonEvent(event);
        logIngestionService.ingest(event, acknowledgment);
    }

    /**
     * Rejects structurally invalid events and events explicitly flagged with {@code force_dlq}.
     * Throwing here lets the container error handler apply the retry/backoff policy and, once
     * exhausted, route the record to the DLQ topic.
     */
    private void rejectPoisonEvent(EventSchema event) {
        if (event == null) {
            throw new LogEventProcessingException("Received null log event");
        }
        if (event.eventId() == null || event.eventId().isBlank()) {
            throw new LogEventProcessingException("Log event is missing eventId");
        }
        if (event.serviceName() == null || event.serviceName().isBlank()) {
            throw new LogEventProcessingException("Log event " + event.eventId() + " is missing serviceName");
        }
        if (Boolean.TRUE.equals(event.metadata().get(FORCE_DLQ_METADATA_KEY))
                || "true".equalsIgnoreCase(String.valueOf(event.metadata().get(FORCE_DLQ_METADATA_KEY)))) {
            throw new LogEventProcessingException("Log event " + event.eventId() + " flagged for DLQ via force_dlq");
        }
    }
}
