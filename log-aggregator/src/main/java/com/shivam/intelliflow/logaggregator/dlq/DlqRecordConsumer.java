package com.shivam.intelliflow.logaggregator.dlq;

import com.shivam.intelliflow.common.constants.KafkaTopicConstants;
import com.shivam.intelliflow.common.event.EventSchema;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

/**
 * Consumes the {@code dlq} topic and persists each permanently failed message so it can be
 * inspected and replayed. Failures here are retried by the container error handler but are
 * never routed back onto the DLQ topic to avoid infinite loops.
 */
@Component
public class DlqRecordConsumer {
    private final DlqPersistenceService persistenceService;

    public DlqRecordConsumer(DlqPersistenceService persistenceService) {
        this.persistenceService = persistenceService;
    }

    @KafkaListener(
            id = "log-aggregator-dlq-consumer",
            topics = KafkaTopicConstants.DLQ,
            containerFactory = "dlqKafkaListenerContainerFactory",
            autoStartup = "${intelliflow.log-aggregator.dlq.consumer.enabled:true}"
    )
    public void consume(EventSchema envelope, Acknowledgment acknowledgment) {
        persistenceService.persist(envelope);
        acknowledgment.acknowledge();
    }
}
