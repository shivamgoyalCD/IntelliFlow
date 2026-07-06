package com.shivam.intelliflow.logaggregator.config;

import com.shivam.intelliflow.common.event.EventSchema;
import com.shivam.intelliflow.common.kafka.EventSchemaKafkaSerdes;
import java.util.HashMap;
import java.util.Map;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.CooperativeStickyAssignor;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

@EnableKafka
@Configuration(proxyBeanMethods = false)
public class LogAggregatorKafkaConfig {
    private final String bootstrapServers;
    private final String groupId;
    private final String autoOffsetReset;
    private final int maxPollRecords;
    private final int concurrency;
    private final long errorBackoffMs;
    private final long errorMaxAttempts;

    public LogAggregatorKafkaConfig(
            @Value("${spring.kafka.bootstrap-servers:localhost:9092}") String bootstrapServers,
            @Value("${spring.kafka.consumer.group-id:log-aggregator}") String groupId,
            @Value("${spring.kafka.consumer.auto-offset-reset:latest}") String autoOffsetReset,
            @Value("${intelliflow.log-aggregator.consumer.max-poll-records:100}") int maxPollRecords,
            @Value("${intelliflow.log-aggregator.consumer.concurrency:1}") int concurrency,
            @Value("${intelliflow.log-aggregator.consumer.error-backoff-ms:1000}") long errorBackoffMs,
            @Value("${intelliflow.log-aggregator.consumer.error-max-attempts:3}") long errorMaxAttempts
    ) {
        this.bootstrapServers = bootstrapServers;
        this.groupId = groupId;
        this.autoOffsetReset = autoOffsetReset;
        this.maxPollRecords = maxPollRecords;
        this.concurrency = concurrency;
        this.errorBackoffMs = errorBackoffMs;
        this.errorMaxAttempts = errorMaxAttempts;
    }

    @Bean
    public ConsumerFactory<String, EventSchema> logAggregatorEventConsumerFactory() {
        return new DefaultKafkaConsumerFactory<>(
                consumerProperties(),
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
        return factory;
    }

    @Bean
    public CommonErrorHandler logAggregatorErrorHandler(LogAggregatorDlqRecoverer recoverer) {
        DefaultErrorHandler errorHandler = new DefaultErrorHandler(
                recoverer,
                new FixedBackOff(errorBackoffMs, retryAttempts())
        );
        errorHandler.setAckAfterHandle(false);
        errorHandler.setCommitRecovered(false);
        return errorHandler;
    }

    private Map<String, Object> consumerProperties() {
        Map<String, Object> properties = new HashMap<>();
        properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        properties.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, EventSchemaKafkaSerdes.deserializer().getClass());
        properties.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, autoOffsetReset);
        properties.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, maxPollRecords);
        properties.put(ConsumerConfig.PARTITION_ASSIGNMENT_STRATEGY_CONFIG, CooperativeStickyAssignor.class.getName());
        return properties;
    }

    private long retryAttempts() {
        return Math.max(0, errorMaxAttempts - 1);
    }
}
