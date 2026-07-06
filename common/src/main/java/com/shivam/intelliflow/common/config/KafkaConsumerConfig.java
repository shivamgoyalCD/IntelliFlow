package com.shivam.intelliflow.common.config;

import com.shivam.intelliflow.common.event.EventSchema;
import com.shivam.intelliflow.common.kafka.EventSchemaKafkaSerdes;
import java.util.HashMap;
import java.util.Map;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;

@EnableKafka
@Configuration(proxyBeanMethods = false)
public class KafkaConsumerConfig {
    private final String bootstrapServers;
    private final String groupId;

    public KafkaConsumerConfig(
            @Value("${spring.kafka.bootstrap-servers:localhost:9092}") String bootstrapServers,
            @Value("${spring.kafka.consumer.group-id:}") String groupId
    ) {
        this.bootstrapServers = bootstrapServers;
        this.groupId = groupId;
    }

    @Bean
    @ConditionalOnMissingBean(name = "eventSchemaConsumerFactory")
    public ConsumerFactory<String, EventSchema> eventSchemaConsumerFactory() {
        return new DefaultKafkaConsumerFactory<>(
                consumerProperties(),
                new StringDeserializer(),
                EventSchemaKafkaSerdes.deserializer()
        );
    }

    @Bean
    @ConditionalOnMissingBean(name = "eventSchemaKafkaListenerContainerFactory")
    public ConcurrentKafkaListenerContainerFactory<String, EventSchema> eventSchemaKafkaListenerContainerFactory(
            ConsumerFactory<String, EventSchema> eventSchemaConsumerFactory
    ) {
        ConcurrentKafkaListenerContainerFactory<String, EventSchema> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(eventSchemaConsumerFactory);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL);
        return factory;
    }

    private Map<String, Object> consumerProperties() {
        Map<String, Object> properties = new HashMap<>();
        properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, EventSchemaKafkaSerdes.deserializer().getClass());
        properties.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);

        if (!groupId.isBlank()) {
            properties.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        }

        return properties;
    }
}
