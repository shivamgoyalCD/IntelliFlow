package com.shivam.intelliflow.logaggregator.dlq;

import com.shivam.intelliflow.common.event.EventSchema;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Translates a DLQ envelope consumed from the {@code dlq} topic into a persistent
 * {@link DlqMessage} row so that failed messages can be inspected and replayed.
 */
@Service
public class DlqPersistenceService {
    private static final Logger LOGGER = LoggerFactory.getLogger(DlqPersistenceService.class);

    private final DlqMessageRepository repository;

    public DlqPersistenceService(DlqMessageRepository repository) {
        this.repository = repository;
    }

    public UUID persist(EventSchema envelope) {
        String payloadJson = DlqEnvelope.string(envelope, DlqEnvelope.META_PAYLOAD_JSON);
        Integer failureCount = DlqEnvelope.integer(envelope, DlqEnvelope.META_FAILURE_COUNT);

        DlqMessage message = new DlqMessage(
                null,
                envelope.eventId(),
                DlqEnvelope.string(envelope, DlqEnvelope.META_ORIGIN_SERVICE),
                requireTopic(DlqEnvelope.string(envelope, DlqEnvelope.META_ORIGINAL_TOPIC)),
                DlqEnvelope.integer(envelope, DlqEnvelope.META_ORIGINAL_PARTITION),
                DlqEnvelope.longValue(envelope, DlqEnvelope.META_ORIGINAL_OFFSET),
                DlqEnvelope.string(envelope, DlqEnvelope.META_ORIGINAL_KEY),
                isJsonObject(payloadJson) ? payloadJson : "{}",
                isJsonObject(payloadJson) ? null : payloadJson,
                DlqEnvelope.string(envelope, DlqEnvelope.META_ERROR_CLASS),
                DlqEnvelope.string(envelope, DlqEnvelope.META_ERROR_MESSAGE),
                failureCount == null ? 1 : failureCount,
                DlqMessageStatus.PENDING,
                DlqEnvelope.instant(envelope, DlqEnvelope.META_FAILED_AT),
                null,
                null,
                null,
                null,
                null
        );

        UUID id = repository.save(message);
        LOGGER.info(
                "Persisted DLQ message id={} originalTopic={} originService={} errorClass={}",
                id,
                message.originalTopic(),
                message.originService(),
                message.errorClass()
        );
        return id;
    }

    private static String requireTopic(String topic) {
        return topic == null || topic.isBlank() ? "unknown" : topic;
    }

    private static boolean isJsonObject(String payload) {
        if (payload == null) {
            return false;
        }
        String trimmed = payload.trim();
        return trimmed.startsWith("{") && trimmed.endsWith("}");
    }
}
