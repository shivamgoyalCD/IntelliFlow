package com.shivam.intelliflow.logaggregator.dlq;

import com.shivam.intelliflow.common.event.EventLevel;
import com.shivam.intelliflow.common.event.EventSchema;
import com.shivam.intelliflow.common.util.JsonUtils;
import com.shivam.intelliflow.common.util.TimestampUtils;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.apache.kafka.clients.consumer.ConsumerRecord;

/**
 * Builds and parses the {@link EventSchema} envelope used to carry a permanently failed
 * message onto the {@code dlq} topic. The original payload and enough failure context to
 * replay the message are preserved inside the envelope metadata.
 */
public final class DlqEnvelope {
    public static final String META_ORIGINAL_TOPIC = "dlq.original_topic";
    public static final String META_ORIGINAL_PARTITION = "dlq.original_partition";
    public static final String META_ORIGINAL_OFFSET = "dlq.original_offset";
    public static final String META_ORIGINAL_KEY = "dlq.original_key";
    public static final String META_ORIGIN_SERVICE = "dlq.origin_service";
    public static final String META_ERROR_CLASS = "dlq.error_class";
    public static final String META_ERROR_MESSAGE = "dlq.error_message";
    public static final String META_FAILURE_COUNT = "dlq.failure_count";
    public static final String META_FAILED_AT = "dlq.failed_at";
    public static final String META_PAYLOAD_JSON = "dlq.payload_json";

    private DlqEnvelope() {
    }

    public static EventSchema forFailure(
            ConsumerRecord<?, ?> record,
            Throwable failure,
            int failureCount,
            String dlqServiceName
    ) {
        Throwable rootCause = unwrap(failure);
        Object value = record.value();
        String originService = value instanceof EventSchema event ? event.serviceName() : dlqServiceName;
        String traceId = value instanceof EventSchema event ? event.traceId() : null;
        String spanId = value instanceof EventSchema event ? event.spanId() : null;

        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put(META_ORIGINAL_TOPIC, record.topic());
        metadata.put(META_ORIGINAL_PARTITION, record.partition());
        metadata.put(META_ORIGINAL_OFFSET, record.offset());
        metadata.put(META_ORIGINAL_KEY, record.key() == null ? null : String.valueOf(record.key()));
        metadata.put(META_ORIGIN_SERVICE, originService);
        metadata.put(META_ERROR_CLASS, rootCause.getClass().getName());
        metadata.put(META_ERROR_MESSAGE, rootCause.getMessage());
        metadata.put(META_FAILURE_COUNT, failureCount);
        metadata.put(META_FAILED_AT, TimestampUtils.nowUtc().toString());
        metadata.put(META_PAYLOAD_JSON, serializePayload(value));

        return new EventSchema(
                UUID.randomUUID().toString(),
                TimestampUtils.nowUtc(),
                dlqServiceName,
                EventLevel.ERROR,
                traceId,
                spanId,
                "DLQ: " + rootCause.getClass().getSimpleName() + ": " + rootCause.getMessage(),
                metadata
        );
    }

    public static String string(EventSchema envelope, String key) {
        Object value = envelope.metadata().get(key);
        return value == null ? null : String.valueOf(value);
    }

    public static Integer integer(EventSchema envelope, String key) {
        Object value = envelope.metadata().get(key);
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.valueOf(String.valueOf(value));
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    public static Long longValue(EventSchema envelope, String key) {
        Object value = envelope.metadata().get(key);
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.valueOf(String.valueOf(value));
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    public static Instant instant(EventSchema envelope, String key) {
        String value = string(envelope, key);
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Instant.parse(value);
        } catch (RuntimeException exception) {
            return null;
        }
    }

    private static String serializePayload(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof String stringValue) {
            return stringValue;
        }
        return JsonUtils.toJson(value);
    }

    private static Throwable unwrap(Throwable failure) {
        Throwable current = Objects.requireNonNullElseGet(failure, () -> new IllegalStateException("unknown failure"));
        while (current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        return current;
    }
}
