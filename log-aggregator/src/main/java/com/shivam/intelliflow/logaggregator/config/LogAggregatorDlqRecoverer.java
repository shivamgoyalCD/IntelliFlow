package com.shivam.intelliflow.logaggregator.config;

import com.shivam.intelliflow.common.constants.KafkaTopicConstants;
import com.shivam.intelliflow.common.event.EventSchema;
import com.shivam.intelliflow.logaggregator.dlq.DlqEnvelope;
import java.nio.ByteBuffer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.ConsumerRecordRecoverer;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.stereotype.Component;

/**
 * Terminal recoverer for the {@code logs} consumer. Once the retry policy is exhausted the
 * offending record is wrapped in a {@link DlqEnvelope} and published to the {@code dlq} topic
 * so it can be persisted and later replayed.
 */
@Component
public class LogAggregatorDlqRecoverer implements ConsumerRecordRecoverer {
    private static final Logger LOGGER = LoggerFactory.getLogger(LogAggregatorDlqRecoverer.class);

    private final KafkaTemplate<String, EventSchema> kafkaTemplate;
    private final String dlqTopic;
    private final String serviceName;
    private final int defaultFailureCount;

    public LogAggregatorDlqRecoverer(
            KafkaTemplate<String, EventSchema> eventSchemaKafkaTemplate,
            @Value("${intelliflow.log-aggregator.dlq.topic:" + KafkaTopicConstants.DLQ + "}") String dlqTopic,
            @Value("${spring.application.name:log-aggregator}") String serviceName,
            @Value("${intelliflow.log-aggregator.consumer.error-max-attempts:3}") int defaultFailureCount
    ) {
        this.kafkaTemplate = eventSchemaKafkaTemplate;
        this.dlqTopic = dlqTopic;
        this.serviceName = serviceName;
        this.defaultFailureCount = defaultFailureCount;
    }

    @Override
    public void accept(ConsumerRecord<?, ?> record, Exception exception) {
        int failureCount = resolveFailureCount(record);
        EventSchema envelope = DlqEnvelope.forFailure(record, exception, failureCount, serviceName);
        String key = record.key() == null ? null : String.valueOf(record.key());

        try {
            kafkaTemplate.send(dlqTopic, key, envelope);
            LOGGER.warn(
                    "Routed permanently failed record to DLQ topic={} from originalTopic={}, partition={}, offset={}, attempts={}",
                    dlqTopic,
                    record.topic(),
                    record.partition(),
                    record.offset(),
                    failureCount,
                    exception
            );
        } catch (RuntimeException dlqFailure) {
            LOGGER.error(
                    "Failed to route record to DLQ topic={} from originalTopic={}, partition={}, offset={}",
                    dlqTopic,
                    record.topic(),
                    record.partition(),
                    record.offset(),
                    dlqFailure
            );
            throw dlqFailure;
        }
    }

    private int resolveFailureCount(ConsumerRecord<?, ?> record) {
        Header header = record.headers().lastHeader(KafkaHeaders.DELIVERY_ATTEMPT);
        if (header != null && header.value() != null && header.value().length == Integer.BYTES) {
            return ByteBuffer.wrap(header.value()).getInt();
        }
        return defaultFailureCount;
    }
}
