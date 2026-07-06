package com.shivam.intelliflow.logaggregator.service;

public class LogEventProcessingException extends RuntimeException {
    public LogEventProcessingException(String message) {
        super(message);
    }

    public LogEventProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}
