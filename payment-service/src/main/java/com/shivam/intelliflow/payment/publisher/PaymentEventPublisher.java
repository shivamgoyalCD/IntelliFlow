package com.shivam.intelliflow.payment.publisher;

import com.shivam.intelliflow.common.constants.KafkaTopicConstants;
import com.shivam.intelliflow.common.event.EventLevel;
import com.shivam.intelliflow.common.event.EventSchema;
import com.shivam.intelliflow.common.util.TimestampUtils;
import com.shivam.intelliflow.payment.config.TraceContextFilter;
import com.shivam.intelliflow.payment.service.PaymentRecord;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class PaymentEventPublisher {
    private static final Logger LOGGER = LoggerFactory.getLogger(PaymentEventPublisher.class);
    private static final String SERVICE_NAME = "payment-service";

    private final KafkaTemplate<String, EventSchema> kafkaTemplate;

    public PaymentEventPublisher(KafkaTemplate<String, EventSchema> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void paymentSucceeded(PaymentRecord paymentRecord, int failureRate) {
        publish(EventLevel.INFO, "payment.success", paymentRecord, failureRate);
    }

    public void paymentFailed(PaymentRecord paymentRecord, int failureRate) {
        publish(EventLevel.ERROR, "payment.failed", paymentRecord, failureRate);
    }

    public void paymentRefunded(PaymentRecord paymentRecord, int failureRate) {
        publish(EventLevel.INFO, "payment.refunded", paymentRecord, failureRate);
    }

    private void publish(EventLevel level, String eventName, PaymentRecord paymentRecord, int failureRate) {
        String eventId = UUID.randomUUID().toString();
        EventSchema event = new EventSchema(
                eventId,
                TimestampUtils.nowUtc(),
                SERVICE_NAME,
                level,
                traceId(),
                spanId(),
                eventName,
                metadata(eventName, paymentRecord, failureRate)
        );

        kafkaTemplate.send(KafkaTopicConstants.LOGS, paymentRecord.id().toString(), event)
                .whenComplete((result, exception) -> {
                    if (exception != null) {
                        LOGGER.warn("Failed to publish payment event {}", eventId, exception);
                    }
                });
    }

    private Map<String, Object> metadata(String eventName, PaymentRecord paymentRecord, int failureRate) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("event_name", eventName);
        metadata.put("payment_id", paymentRecord.id().toString());
        metadata.put("order_id", paymentRecord.orderId().toString());
        metadata.put("amount", paymentRecord.amount());
        metadata.put("currency", paymentRecord.currency());
        metadata.put("payment_method", paymentRecord.paymentMethod());
        metadata.put("status", paymentRecord.status().name());
        metadata.put("latency_ms", paymentRecord.latencyMs());
        metadata.put("failure_rate_percent", failureRate);
        if (paymentRecord.failureReason() != null) {
            metadata.put("failure_reason", paymentRecord.failureReason());
        }
        return metadata;
    }

    private String traceId() {
        String traceId = MDC.get(TraceContextFilter.TRACE_ID);
        return traceId == null || traceId.isBlank() ? UUID.randomUUID().toString() : traceId;
    }

    private String spanId() {
        String spanId = MDC.get(TraceContextFilter.SPAN_ID);
        return spanId == null || spanId.isBlank() ? UUID.randomUUID().toString() : spanId;
    }
}
