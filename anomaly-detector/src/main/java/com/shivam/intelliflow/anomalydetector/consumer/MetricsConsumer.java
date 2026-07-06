package com.shivam.intelliflow.anomalydetector.consumer;

import com.shivam.intelliflow.anomalydetector.service.MetricDetectionService;
import com.shivam.intelliflow.common.constants.KafkaTopicConstants;
import com.shivam.intelliflow.common.event.EventSchema;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
public class MetricsConsumer {
    private final MetricDetectionService metricDetectionService;

    public MetricsConsumer(MetricDetectionService metricDetectionService) {
        this.metricDetectionService = metricDetectionService;
    }

    @KafkaListener(
            id = "anomaly-detector-metrics-consumer",
            topics = KafkaTopicConstants.METRICS,
            containerFactory = "eventSchemaKafkaListenerContainerFactory",
            autoStartup = "${intelliflow.anomaly-detector.consumer.enabled:true}"
    )
    public void consume(EventSchema event, Acknowledgment acknowledgment) {
        metricDetectionService.evaluate(event);
        acknowledgment.acknowledge();
    }
}
