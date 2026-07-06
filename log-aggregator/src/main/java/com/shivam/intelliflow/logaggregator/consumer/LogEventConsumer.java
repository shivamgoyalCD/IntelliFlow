package com.shivam.intelliflow.logaggregator.consumer;

import com.shivam.intelliflow.common.constants.KafkaTopicConstants;
import com.shivam.intelliflow.common.event.EventSchema;
import com.shivam.intelliflow.logaggregator.service.LogIngestionService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
public class LogEventConsumer {
    private final LogIngestionService logIngestionService;

    public LogEventConsumer(LogIngestionService logIngestionService) {
        this.logIngestionService = logIngestionService;
    }

    @KafkaListener(
            id = "log-aggregator-logs-consumer",
            topics = KafkaTopicConstants.LOGS,
            containerFactory = "logAggregatorKafkaListenerContainerFactory",
            autoStartup = "${intelliflow.log-aggregator.consumer.enabled:true}"
    )
    public void consume(EventSchema event, Acknowledgment acknowledgment) {
        logIngestionService.ingest(event, acknowledgment);
    }
}
