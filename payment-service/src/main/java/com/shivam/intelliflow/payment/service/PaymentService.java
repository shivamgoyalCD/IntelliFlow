package com.shivam.intelliflow.payment.service;

import com.shivam.intelliflow.payment.dto.PaymentResponse;
import com.shivam.intelliflow.payment.dto.ProcessPaymentRequest;
import com.shivam.intelliflow.payment.publisher.PaymentEventPublisher;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PaymentService {
    private static final String SIMULATED_FAILURE_REASON = "simulated_payment_failure";

    private final ConcurrentMap<UUID, PaymentRecord> payments = new ConcurrentHashMap<>();
    private final PaymentEventPublisher paymentEventPublisher;
    private final int failureRate;

    public PaymentService(
            PaymentEventPublisher paymentEventPublisher,
            @Value("${intelliflow.payment.failure-rate:${PAYMENT_FAILURE_RATE:5}}") int failureRate
    ) {
        this.paymentEventPublisher = paymentEventPublisher;
        this.failureRate = normalizeFailureRate(failureRate);
    }

    public PaymentResponse processPayment(ProcessPaymentRequest request) {
        Instant startedAt = Instant.now();
        UUID paymentId = UUID.randomUUID();
        boolean failed = shouldFail();
        PaymentStatus status = failed ? PaymentStatus.FAILED : PaymentStatus.SUCCESS;
        String failureReason = failed ? SIMULATED_FAILURE_REASON : null;
        long latencyMs = elapsedMillis(startedAt);

        PaymentRecord paymentRecord = new PaymentRecord(
                paymentId,
                request.orderId(),
                request.amount(),
                request.currency(),
                request.paymentMethod(),
                status,
                failureReason,
                latencyMs,
                startedAt,
                Instant.now()
        );
        payments.put(paymentId, paymentRecord);

        if (failed) {
            paymentEventPublisher.paymentFailed(paymentRecord, failureRate);
        } else {
            paymentEventPublisher.paymentSucceeded(paymentRecord, failureRate);
        }

        return PaymentResponse.from(paymentRecord);
    }

    public PaymentResponse refundPayment(UUID id) {
        Instant startedAt = Instant.now();
        PaymentRecord existingPayment = getPaymentRecord(id);

        if (existingPayment.status() == PaymentStatus.FAILED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Failed payments cannot be refunded");
        }
        if (existingPayment.status() == PaymentStatus.REFUNDED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Payment is already refunded");
        }

        PaymentRecord refundedPayment = existingPayment.withRefundedStatus(elapsedMillis(startedAt));
        payments.put(id, refundedPayment);
        paymentEventPublisher.paymentRefunded(refundedPayment, failureRate);
        return PaymentResponse.from(refundedPayment);
    }

    public PaymentResponse getPayment(UUID id) {
        return PaymentResponse.from(getPaymentRecord(id));
    }

    private PaymentRecord getPaymentRecord(UUID id) {
        PaymentRecord paymentRecord = payments.get(id);
        if (paymentRecord == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Payment not found");
        }
        return paymentRecord;
    }

    private boolean shouldFail() {
        return ThreadLocalRandom.current().nextInt(100) < failureRate;
    }

    private long elapsedMillis(Instant startedAt) {
        return Math.max(1, Duration.between(startedAt, Instant.now()).toMillis());
    }

    private int normalizeFailureRate(int configuredFailureRate) {
        if (configuredFailureRate < 0) {
            return 0;
        }
        return Math.min(configuredFailureRate, 100);
    }
}
