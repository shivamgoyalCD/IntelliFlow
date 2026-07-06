package com.shivam.intelliflow.logaggregator.dlq;

public enum DlqMessageStatus {
    PENDING,
    REPLAYED,
    DISCARDED
}
