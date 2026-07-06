package com.shivam.intelliflow.payment.dto;

import com.shivam.intelliflow.payment.service.PaymentRecord;
import com.shivam.intelliflow.payment.service.PaymentStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentResponse(
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
    public static PaymentResponse from(PaymentRecord paymentRecord) {
        return new PaymentResponse(
                paymentRecord.id(),
                paymentRecord.orderId(),
                paymentRecord.amount(),
                paymentRecord.currency(),
                paymentRecord.paymentMethod(),
                paymentRecord.status(),
                paymentRecord.failureReason(),
                paymentRecord.latencyMs(),
                paymentRecord.createdAt(),
                paymentRecord.updatedAt()
        );
    }
}
