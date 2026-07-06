package com.shivam.intelliflow.logaggregator.config;

import com.shivam.intelliflow.common.config.KafkaProducerConfig;
import com.shivam.intelliflow.common.event.EventSchema;
import com.shivam.intelliflow.common.kafka.EventSchemaKafkaSerdes;
import java.util.HashMap;
import java.util.Map;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.CooperativeStickyAssignor;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.ExponentialBackOffWithMaxRetries;

@EnableKafka
@Configuration(proxyBeanMethods = false)
@Import(KafkaProducerConfig.class)
public class LogAggregatorKafkaConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger(LogAggregatorKafkaConfig.class);

    private final String bootstrapServers;
    private final String groupId;
    private final String autoOffsetReset;
    private final int maxPollRecords;
    private final int concurrency;
    private final long errorBackoffMs;
    private final int errorMaxAttempts;
    private final double errorBackoffMultiplier;
    private final long errorMaxBackoffMs;
    private final String dlqGroupId;
    private final long dlqBackoffMs;
    private final int dlqMaxAttempts;

    public LogAggregatorKafkaConfig(
            @Value("${spring.kafka.bootstrap-servers:localhost:9092}") String bootstrapServers,
            @Value("${spring.kafka.consumer.group-id:log-aggregator}") String groupId,
            @Value("${spring.kafka.consumer.auto-offset-reset:latest}") String autoOffsetReset,
            @Value("${intelliflow.log-aggregator.consumer.max-poll-records:100}") int maxPollRecords,
            @Value("${intelliflow.log-aggregator.consumer.concurrency:1}") int concurrency,
            @Value("${intelliflow.log-aggregator.consumer.error-backoff-ms:1000}") long errorBackoffMs,
            @Value("${intelliflow.log-aggregator.consumer.error-max-attempts:3}") int errorMaxAttempts,
            @Value("${intelliflow.log-aggregator.consumer.error-backoff-multiplier:2.0}") double errorBackoffMultiplier,
            @Value("${intelliflow.log-aggregator.consumer.error-max-backoff-ms:10000}") long errorMaxBackoffMs,
            @Value("${intelliflow.log-aggregator.dlq.consumer.group-id:log-aggregator-dlq}") String dlqGroupId,
            @Value("${intelliflow.log-aggregator.dlq.consumer.error-backoff-ms:1000}") long dlqBackoffMs,
            @Value("${intelliflow.log-aggregator.dlq.consumer.error-max-attempts:3}") int dlqMaxAttempts
    ) {
        this.bootstrapServers = bootstrapServers;
        this.groupId = groupId;
        this.autoOffsetReset = autoOffsetReset;
        this.maxPollRecords = maxPollRecords;
        this.concurrency = concurrency;
        this.errorBackoffMs = errorBackoffMs;
        this.errorMaxAttempts = errorMaxAttempts;
        this.errorBackoffMultiplier = errorBackoffMultiplier;
        this.errorMaxBackoffMs = errorMaxBackoffMs;
        this.dlqGroupId = dlqGroupId;
        this.dlqBackoffMs = dlqBackoffMs;
        this.dlqMaxAttempts = dlqMaxAttempts;
    }

    @Bean
    public ConsumerFactory<String, EventSchema> logAggregatorEventConsumerFactory() {
        return new DefaultKafkaConsumerFactory<>(
                consumerProperties(groupId),
                new StringDeserializer(),
                EventSchemaKafkaSerdes.deserializer()
        );
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, EventSchema> logAggregatorKafkaListenerContainerFactory(
            ConsumerFactory<String, EventSchema> logAggregatorEventConsumerFactory,
            CommonErrorHandler logAggregatorErrorHandler
    ) {
        ConcurrentKafkaListenerContainerFactory<String, EventSchema> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(logAggregatorEventConsumerFactory);
        factory.setConcurrency(concurrency);
        factory.setCommonErrorHandler(logAggregatorErrorHandler);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
        factory.getContainerProperties().setAsyncAcks(true);
        factory.getContainerProperties().setDeliveryAttemptHeader(true);
        return factory;
    }

    @Bean
    public CommonErrorHandler logAggregatorErrorHandler(LogAggregatorDlqRecoverer recoverer) {
        DefaultErrorHandler errorHandler = new DefaultErrorHandler(recoverer, exponentialBackOff(errorMaxAttempts));
        errorHandler.setAckAfterHandle(false);
        errorHandler.setCommitRecovered(false);
        return errorHandler;
    }

    @Bean
    public ConsumerFactory<String, EventSchema> dlqEventConsumerFactory() {
        return new DefaultKafkaConsumerFactory<>(
                consumerProperties(dlqGroupId),
                new StringDeserializer(),
                EventSchemaKafkaSerdes.deserializer()
        );
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, EventSchema> dlqKafkaListenerContainerFactory(
            ConsumerFactory<String, EventSchema> dlqEventConsumerFactory
    ) {
        ConcurrentKafkaListenerContainerFactory<String, EventSchema> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(dlqEventConsumerFactory);
        factory.setConcurrency(1);
        factory.setCommonErrorHandler(dlqErrorHandler());
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
        return factory;
    }

    private CommonErrorHandler dlqErrorHandler() {
        // The DLQ consumer never re-routes onto the DLQ topic; exhausted records are logged and skipped.
        DefaultErrorHandler errorHandler = new DefaultErrorHandler(
                (record, exception) -> LOGGER.error(
                        "Failed to persist DLQ record from topic={}, partition={}, offset={}; skipping",
                        record.topic(),
                        record.partition(),
                        record.offset(),
                        exception
                ),
                exponentialBackOff(dlqMaxAttempts, dlqBackoffMs)
        );
        errorHandler.setAckAfterHandle(false);
        return errorHandler;
    }

    private ExponentialBackOffWithMaxRetries exponentialBackOff(int maxAttempts) {
        return exponentialBackOff(maxAttempts, errorBackoffMs);
    }

    private ExponentialBackOffWithMaxRetries exponentialBackOff(int maxAttempts, long initialIntervalMs) {
        ExponentialBackOffWithMaxRetries backOff = new ExponentialBackOffWithMaxRetries(Math.max(0, maxAttempts - 1));
        backOff.setInitialInterval(Math.max(0, initialIntervalMs));
        backOff.setMultiplier(Math.max(1.0, errorBackoffMultiplier));
        backOff.setMaxInterval(Math.max(initialIntervalMs, errorMaxBackoffMs));
        return backOff;
    }

    private Map<String, Object> consumerProperties(String consumerGroupId) {
        Map<String, Object> properties = new HashMap<>();
        properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        properties.put(ConsumerConfig.GROUP_ID_CONFIG, consumerGroupId);
        properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, EventSchemaKafkaSerdes.deserializer().getClass());
        properties.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, autoOffsetReset);
        properties.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, maxPollRecords);
        properties.put(ConsumerConfig.PARTITION_ASSIGNMENT_STRATEGY_CONFIG, CooperativeStickyAssignor.class.getName());
        return properties;
    }
}
