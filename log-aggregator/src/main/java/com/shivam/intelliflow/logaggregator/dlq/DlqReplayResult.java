package com.shivam.intelliflow.logaggregator.dlq;

import java.util.UUID;

public record DlqReplayResult(UUID id, String topic, String key) {
}
