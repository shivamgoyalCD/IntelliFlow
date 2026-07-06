package com.shivam.intelliflow.order.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record PaymentProcessRequest(
        UUID orderId,
        BigDecimal amount,
        String currency,
        String paymentMethod
) {
}
