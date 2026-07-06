package com.shivam.intelliflow.logaggregator.dlq;

public class DlqReplayException extends RuntimeException {
    public DlqReplayException(String message) {
        super(message);
    }

    public DlqReplayException(String message, Throwable cause) {
        super(message, cause);
    }
}
