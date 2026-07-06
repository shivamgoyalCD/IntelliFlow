package com.shivam.intelliflow.logaggregator.persistence;

import com.shivam.intelliflow.logaggregator.model.LogEventBatch;

public interface LogEventBatchFailureHandler {
    void handleFailure(LogEventBatch batch, RuntimeException exception);
}
