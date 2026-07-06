package com.shivam.intelliflow.logaggregator.dlq;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DlqMessageRepository {
    UUID save(DlqMessage message);

    Optional<DlqMessage> findById(UUID id);

    List<DlqMessage> findByStatus(DlqMessageStatus status, int limit);

    List<DlqMessage> findRecent(int limit);

    void markReplayed(UUID id, String replayTopic);

    void markReplayFailed(UUID id, String error);
}
