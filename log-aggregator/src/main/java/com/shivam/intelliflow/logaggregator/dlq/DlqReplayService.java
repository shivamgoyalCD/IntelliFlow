package com.shivam.intelliflow.logaggregator.dlq;

import com.shivam.intelliflow.common.event.EventSchema;
import com.shivam.intelliflow.common.util.JsonUtils;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

/**
 * Replays a persisted DLQ message back onto its original topic. The original payload is
 * rehydrated from the stored JSON and republished with its original key so downstream
 * consumers reprocess it exactly as if it had never failed.
 */
@Service
public class DlqReplayService {
    private static final Logger LOGGER = LoggerFactory.getLogger(DlqReplayService.class);

    private final DlqMessageRepository repository;
    private final KafkaTemplate<String, EventSchema> kafkaTemplate;
    private final long sendTimeoutSeconds;

    public DlqReplayService(
            DlqMessageRepository repository,
            KafkaTemplate<String, EventSchema> eventSchemaKafkaTemplate,
            @Value("${intelliflow.log-aggregator.dlq.replay.send-timeout-seconds:10}") long sendTimeoutSeconds
    ) {
        this.repository = repository;
        this.kafkaTemplate = eventSchemaKafkaTemplate;
        this.sendTimeoutSeconds = sendTimeoutSeconds;
    }

    public DlqReplayResult replay(UUID id) {
        DlqMessage message = repository.findById(id)
                .orElseThrow(() -> new DlqMessageNotFoundException(id));

        if (message.status() == DlqMessageStatus.REPLAYED) {
            throw new DlqReplayException("DLQ message " + id + " has already been replayed");
        }

        EventSchema payload = deserializePayload(message);
        String topic = message.originalTopic();
        String key = message.messageKey();

        try {
            kafkaTemplate.send(topic, key, payload).get(sendTimeoutSeconds, TimeUnit.SECONDS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            repository.markReplayFailed(id, exception.getMessage());
            throw new DlqReplayException("Interrupted while replaying DLQ message " + id, exception);
        } catch (RuntimeException | ExecutionException | TimeoutException exception) {
            repository.markReplayFailed(id, exception.getMessage());
            throw new DlqReplayException("Failed to replay DLQ message " + id + " to topic " + topic, exception);
        }

        repository.markReplayed(id, topic);
        LOGGER.info("Replayed DLQ message id={} to topic={} key={}", id, topic, key);
        return new DlqReplayResult(id, topic, key);
    }

    private EventSchema deserializePayload(DlqMessage message) {
        String payload = message.payload();
        if (payload == null || payload.isBlank() || "{}".equals(payload.trim())) {
            payload = message.payloadRaw();
        }
        if (payload == null || payload.isBlank()) {
            throw new DlqReplayException("DLQ message " + message.id() + " has no replayable payload");
        }
        try {
            return JsonUtils.fromJson(payload, EventSchema.class);
        } catch (RuntimeException exception) {
            throw new DlqReplayException(
                    "DLQ message " + message.id() + " payload could not be parsed as an event", exception);
        }
    }
}
