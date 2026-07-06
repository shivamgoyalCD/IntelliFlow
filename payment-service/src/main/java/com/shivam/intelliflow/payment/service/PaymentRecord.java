package com.shivam.intelliflow.payment.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentRecord(
        UUID id,
        UUID orderId,
        BigDecimal amount,
        String currency,
        String paymentMethod,
        PaymentStatus status,
        String failureReason,
        long latencyMs,
        Instant createdAt,
        Instant updatedAt
) {
    public PaymentRecord withRefundedStatus(long refundLatencyMs) {
        return new PaymentRecord(
                id,
                orderId,
                amount,
                currency,
                paymentMethod,
                PaymentStatus.REFUNDED,
                failureReason,
                refundLatencyMs,
                createdAt,
                Instant.now()
        );
    }
}
