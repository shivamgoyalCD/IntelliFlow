package com.shivam.intelliflow.logaggregator.persistence;

import com.shivam.intelliflow.logaggregator.model.LogEventBatch;

public interface LogEventStore {
    void saveBatch(LogEventBatch batch);
}
