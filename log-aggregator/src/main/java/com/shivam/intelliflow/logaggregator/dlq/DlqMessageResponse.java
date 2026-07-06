package com.shivam.intelliflow.logaggregator.dlq;

import java.time.Instant;
import java.util.UUID;

public record DlqMessageResponse(
        UUID id,
        String originService,
        String originalTopic,
        Integer originalPartition,
        Long originalOffset,
        String messageKey,
        String errorClass,
        String errorMessage,
        int failureCount,
        DlqMessageStatus status,
        Instant failedAt,
        String replayTopic,
        Instant replayedAt,
        String lastError,
        Instant createdAt
) {
    public static DlqMessageResponse from(DlqMessage message) {
        return new DlqMessageResponse(
                message.id(),
                message.originService(),
                message.originalTopic(),
                message.originalPartition(),
                message.originalOffset(),
                message.messageKey(),
                message.errorClass(),
                message.errorMessage(),
                message.failureCount(),
                message.status(),
                message.failedAt(),
                message.replayTopic(),
                message.replayedAt(),
                message.lastError(),
                message.createdAt()
        );
    }
}
