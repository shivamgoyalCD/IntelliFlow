package com.shivam.intelliflow.order.dto;

import java.util.UUID;

public record PaymentProcessResponse(
        UUID id,
        UUID orderId,
        String status,
        String failureReason,
        long latencyMs
) {
    public boolean successful() {
        return "SUCCESS".equals(status);
    }
}
