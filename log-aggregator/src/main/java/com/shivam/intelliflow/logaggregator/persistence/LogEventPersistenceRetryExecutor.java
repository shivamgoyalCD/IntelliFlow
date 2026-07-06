package com.shivam.intelliflow.logaggregator.persistence;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class LogEventPersistenceRetryExecutor {
    private final int maxAttempts;
    private final long backoffMs;

    public LogEventPersistenceRetryExecutor(
            @Value("${intelliflow.log-aggregator.persistence.retry.max-attempts:3}") int maxAttempts,
            @Value("${intelliflow.log-aggregator.persistence.retry.backoff-ms:100}") long backoffMs
    ) {
        this.maxAttempts = Math.max(1, maxAttempts);
        this.backoffMs = Math.max(0, backoffMs);
    }

    public void execute(Runnable operation) {
        RuntimeException lastFailure = null;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                operation.run();
                return;
            } catch (RuntimeException exception) {
                lastFailure = exception;
                if (attempt < maxAttempts) {
                    pauseBeforeRetry();
                }
            }
        }

        throw new LogEventPersistenceException("Failed to persist log event batch after retries", lastFailure);
    }

    private void pauseBeforeRetry() {
        if (backoffMs == 0) {
            return;
        }

        try {
            Thread.sleep(backoffMs);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new LogEventPersistenceException("Interrupted while waiting to retry log event persistence", exception);
        }
    }
}
