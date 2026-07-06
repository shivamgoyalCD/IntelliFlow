package com.shivam.intelliflow.anomalydetector.publisher;

import com.shivam.intelliflow.anomalydetector.model.AlertEvent;
import com.shivam.intelliflow.anomalydetector.model.AlertSeverity;
import com.shivam.intelliflow.anomalydetector.model.AnomalyMetadata;
import com.shivam.intelliflow.common.constants.KafkaTopicConstants;
import com.shivam.intelliflow.common.event.EventLevel;
import com.shivam.intelliflow.common.event.EventSchema;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class AlertEventPublisher {
    private static final String SERVICE_NAME = "anomaly-detector";

    private final KafkaTemplate<String, EventSchema> kafkaTemplate;
    private final long publishTimeoutSeconds;

    public AlertEventPublisher(
            KafkaTemplate<String, EventSchema> kafkaTemplate,
            @Value("${intelliflow.anomaly-detector.publisher.timeout-seconds:5}") long publishTimeoutSeconds
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.publishTimeoutSeconds = publishTimeoutSeconds;
    }

    public void publish(AlertEvent alertEvent) {
        try {
            kafkaTemplate.send(
                    KafkaTopicConstants.ALERTS,
                    alertEvent.serviceName(),
                    toEventSchema(alertEvent)
            ).get(publishTimeoutSeconds, TimeUnit.SECONDS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while publishing alert event", exception);
        } catch (ExecutionException | TimeoutException exception) {
            throw new IllegalStateException("Failed to publish alert event", exception);
        }
    }

    private EventSchema toEventSchema(AlertEvent alertEvent) {
        return new EventSchema(
                alertEvent.alertId().toString(),
                alertEvent.timestamp(),
                SERVICE_NAME,
                eventLevel(alertEvent.severity()),
                alertEvent.traceId(),
                alertEvent.spanId(),
                alertEvent.message(),
                metadata(alertEvent)
        );
    }

    private EventLevel eventLevel(AlertSeverity severity) {
        return switch (severity) {
            case INFO -> EventLevel.INFO;
            case WARN -> EventLevel.WARN;
            case ERROR -> EventLevel.ERROR;
            case FATAL -> EventLevel.FATAL;
        };
    }

    private Map<String, Object> metadata(AlertEvent alertEvent) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("alert_id", alertEvent.alertId().toString());
        metadata.put("alert_type", alertEvent.anomalyType().name());
        metadata.put("anomaly_type", alertEvent.anomalyType().name());
        if (alertEvent.ruleType() != null) {
            metadata.put("rule_type", alertEvent.ruleType().name());
        }
        if (alertEvent.ruleId() != null) {
            metadata.put("rule_id", alertEvent.ruleId().toString());
        }
        if (alertEvent.ruleName() != null) {
            metadata.put("rule_name", alertEvent.ruleName());
        }
        metadata.put("service_name", alertEvent.serviceName());
        metadata.put("severity", alertEvent.severity().name());
        metadata.put("observed_value", alertEvent.observedValue());
        metadata.put("threshold_value", alertEvent.thresholdValue());
        addAnomalyMetadata(metadata, alertEvent.anomalyMetadata());
        metadata.putAll(alertEvent.metadata());
        return metadata;
    }

    private void addAnomalyMetadata(Map<String, Object> metadata, AnomalyMetadata anomalyMetadata) {
        if (anomalyMetadata == null) {
            return;
        }
        metadata.put("metric_name", anomalyMetadata.metricName());
        if (anomalyMetadata.baselineMean() != null) {
            metadata.put("baseline_mean", anomalyMetadata.baselineMean());
        }
        if (anomalyMetadata.baselineStdDev() != null) {
            metadata.put("baseline_std_dev", anomalyMetadata.baselineStdDev());
        }
        if (anomalyMetadata.zScore() != null) {
            metadata.put("z_score", anomalyMetadata.zScore());
        }
        if (anomalyMetadata.sampleCount() != null) {
            metadata.put("sample_count", anomalyMetadata.sampleCount());
        }
        metadata.putAll(anomalyMetadata.details());
    }
}
