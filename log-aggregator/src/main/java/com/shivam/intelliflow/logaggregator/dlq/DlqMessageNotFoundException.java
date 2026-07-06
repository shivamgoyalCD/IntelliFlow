package com.shivam.intelliflow.logaggregator.dlq;

import java.util.UUID;

public class DlqMessageNotFoundException extends RuntimeException {
    public DlqMessageNotFoundException(UUID id) {
        super("DLQ message not found: " + id);
    }
}
