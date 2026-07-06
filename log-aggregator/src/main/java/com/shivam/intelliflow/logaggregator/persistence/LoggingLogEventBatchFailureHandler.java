package com.shivam.intelliflow.logaggregator.persistence;

import com.shivam.intelliflow.logaggregator.model.LogEventBatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class LoggingLogEventBatchFailureHandler implements LogEventBatchFailureHandler {
    private static final Logger log = LoggerFactory.getLogger(LoggingLogEventBatchFailureHandler.class);

    @Override
    public void handleFailure(LogEventBatch batch, RuntimeException exception) {
        log.warn(
                "Failed to persist log event batch with {} event(s); records remain uncommitted for retry or future DLQ handling",
                batch.size(),
                exception
        );
    }
}
