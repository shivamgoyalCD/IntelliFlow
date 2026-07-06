package com.shivam.intelliflow.auth.publisher;

import com.shivam.intelliflow.auth.entity.AuthUser;
import com.shivam.intelliflow.auth.security.TraceContextFilter;
import com.shivam.intelliflow.common.constants.KafkaTopicConstants;
import com.shivam.intelliflow.common.event.EventLevel;
import com.shivam.intelliflow.common.event.EventSchema;
import com.shivam.intelliflow.common.util.TimestampUtils;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class AuthEventPublisher {
    private static final Logger LOGGER = LoggerFactory.getLogger(AuthEventPublisher.class);
    private static final String SERVICE_NAME = "auth-service";

    private final KafkaTemplate<String, EventSchema> kafkaTemplate;

    public AuthEventPublisher(KafkaTemplate<String, EventSchema> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void loginSuccess(AuthUser user) {
        publish(EventLevel.INFO, "auth.login.success", user.getUsername(), Map.of(
                "user_id", user.getId().toString(),
                "role", user.getRole()
        ));
    }

    public void loginFailed(String username, String reason) {
        publish(EventLevel.WARN, "auth.login.failed", username, Map.of("reason", reason));
    }

    public void logout(String username) {
        publish(EventLevel.INFO, "auth.logout", username, Map.of());
    }

    public void tokenRefreshed(AuthUser user) {
        publish(EventLevel.INFO, "auth.token.refreshed", user.getUsername(), Map.of(
                "user_id", user.getId().toString(),
                "role", user.getRole()
        ));
    }

    public void validationFailed(String username, String reason) {
        publish(EventLevel.WARN, "auth.validation.failed", username, Map.of("reason", reason));
    }

    private void publish(EventLevel level, String eventName, String username, Map<String, Object> details) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("event_name", eventName);
        metadata.put("username", username);
        metadata.putAll(details);

        String eventId = UUID.randomUUID().toString();
        EventSchema event = new EventSchema(
                eventId,
                TimestampUtils.nowUtc(),
                SERVICE_NAME,
                level,
                traceId(),
                spanId(),
                eventName,
                metadata
        );

        try {
            kafkaTemplate.send(KafkaTopicConstants.LOGS, username == null ? eventId : username, event)
                    .whenComplete((result, exception) -> {
                        if (exception != null) {
                            LOGGER.warn("Failed to publish auth event {}", eventId, exception);
                        }
                    });
        } catch (RuntimeException exception) {
            LOGGER.warn("Failed to enqueue auth event {}", eventId, exception);
        }
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
