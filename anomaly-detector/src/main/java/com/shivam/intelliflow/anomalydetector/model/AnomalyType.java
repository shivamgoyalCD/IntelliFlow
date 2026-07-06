package com.shivam.intelliflow.anomalydetector.model;

public enum AnomalyType {
    THRESHOLD_ERROR_RATE,
    THRESHOLD_LATENCY_P99,
    THRESHOLD_THROUGHPUT_DROP,
    THRESHOLD_CONSECUTIVE_ERRORS,
    Z_SCORE
}
