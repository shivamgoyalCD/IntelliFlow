package com.shivam.intelliflow.logaggregator.dlq;

import java.time.Instant;
import java.util.UUID;

/**
 * Persistent view of a permanently failed message captured from the {@code dlq} topic.
 */
public record DlqMessage(
        UUID id,
        String dlqEventId,
        String originService,
        String originalTopic,
        Integer originalPartition,
        Long originalOffset,
        String messageKey,
        String payload,
        String payloadRaw,
        String errorClass,
        String errorMessage,
        int failureCount,
        DlqMessageStatus status,
        Instant failedAt,
        String replayTopic,
        Instant replayedAt,
        String lastError,
        Instant createdAt,
        Instant updatedAt
) {
}
