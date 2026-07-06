package com.shivam.intelliflow.metricscollector.consumer;

import com.shivam.intelliflow.common.constants.KafkaTopicConstants;
import com.shivam.intelliflow.common.event.EventSchema;
import com.shivam.intelliflow.metricscollector.service.MetricsIngestionService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
public class MetricsConsumer {
    private final MetricsIngestionService metricsIngestionService;

    public MetricsConsumer(MetricsIngestionService metricsIngestionService) {
        this.metricsIngestionService = metricsIngestionService;
    }

    @KafkaListener(
            id = "metrics-collector-metrics-consumer",
            topics = KafkaTopicConstants.METRICS,
            containerFactory = "eventSchemaKafkaListenerContainerFactory",
            autoStartup = "${intelliflow.metrics-collector.consumer.enabled:true}"
    )
    public void consume(EventSchema event, Acknowledgment acknowledgment) {
        metricsIngestionService.ingest(event);
        acknowledgment.acknowledge();
    }
}
