package com.shivam.intelliflow.order.publisher;

import com.shivam.intelliflow.common.constants.KafkaTopicConstants;
import com.shivam.intelliflow.common.event.EventLevel;
import com.shivam.intelliflow.common.event.EventSchema;
import com.shivam.intelliflow.common.util.TimestampUtils;
import com.shivam.intelliflow.order.config.TraceContextFilter;
import com.shivam.intelliflow.order.entity.Order;
import com.shivam.intelliflow.order.entity.OrderStatus;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class EventPublisher {
    private static final Logger LOGGER = LoggerFactory.getLogger(EventPublisher.class);
    private static final String SERVICE_NAME = "order-service";

    private final KafkaTemplate<String, EventSchema> kafkaTemplate;
    private final MeterRegistry meterRegistry;

    public EventPublisher(KafkaTemplate<String, EventSchema> kafkaTemplate, MeterRegistry meterRegistry) {
        this.kafkaTemplate = kafkaTemplate;
        this.meterRegistry = meterRegistry;
    }

    public void publishOrderCreated(Order order) {
        publish(
                KafkaTopicConstants.LOGS,
                order,
                EventLevel.INFO,
                "ORDER_CREATED",
                "Order created",
                baseMetadata(order, "ORDER_CREATED")
        );

        Map<String, Object> metadata = baseMetadata(order, "ORDER_CREATED_METRIC");
        metadata.put("metric_name", "orders.created");
        metadata.put("metric_value", 1);
        publish(
                KafkaTopicConstants.METRICS,
                order,
                EventLevel.INFO,
                "ORDER_CREATED_METRIC",
                "Order created metric",
                metadata
        );
    }

    public void publishOrderUpdated(Order order, OrderStatus previousStatus) {
        boolean cancelled = previousStatus != OrderStatus.CANCELLED && order.getStatus() == OrderStatus.CANCELLED;
        publish(
                KafkaTopicConstants.LOGS,
                order,
                cancelled ? EventLevel.WARN : EventLevel.INFO,
                cancelled ? "ORDER_CANCELLED" : "ORDER_UPDATED",
                cancelled ? "Order cancelled" : "Order updated",
                baseMetadata(order, cancelled ? "ORDER_CANCELLED" : "ORDER_UPDATED")
        );
    }

    public void publishOrderDeleted(Order order) {
        publish(
                KafkaTopicConstants.LOGS,
                order,
                EventLevel.INFO,
                "ORDER_DELETED",
                "Order deleted",
                baseMetadata(order, "ORDER_DELETED")
        );
    }

    private void publish(
            String topic,
            Order order,
            EventLevel level,
            String eventType,
            String message,
            Map<String, Object> metadata
    ) {
        String eventId = UUID.randomUUID().toString();
        String orderId = order.getId().toString();

        EventSchema event = new EventSchema(
                eventId,
                TimestampUtils.nowUtc(),
                SERVICE_NAME,
                level,
                traceId(),
                spanId(),
                message,
                metadata
        );

        kafkaTemplate.send(topic, orderId, event)
                .whenComplete((result, exception) -> {
                    String resultTag = exception == null ? "success" : "failure";
                    meterRegistry.counter(
                            "intelliflow.order.kafka.events",
                            "topic", topic,
                            "event_type", eventType,
                            "result", resultTag
                    ).increment();

                    if (exception != null) {
                        LOGGER.warn("Failed to publish order event {} to {}", eventId, topic, exception);
                    }
                });
    }

    private Map<String, Object> baseMetadata(Order order, String operation) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("operation", operation);
        metadata.put("order_id", order.getId().toString());
        metadata.put("customer_id", order.getCustomerId());
        metadata.put("amount", order.getTotalAmount());
        metadata.put("currency", order.getCurrency());
        metadata.put("item_count", order.getItemCount());
        metadata.put("status", order.getStatus().name());
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
