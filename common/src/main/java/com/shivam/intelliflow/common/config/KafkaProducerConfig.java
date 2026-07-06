package com.shivam.intelliflow.common.config;

import com.shivam.intelliflow.common.event.EventSchema;
import com.shivam.intelliflow.common.kafka.EventSchemaKafkaSerdes;
import java.util.HashMap;
import java.util.Map;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

@Configuration(proxyBeanMethods = false)
public class KafkaProducerConfig {
    private final String bootstrapServers;

    public KafkaProducerConfig(
            @Value("${spring.kafka.bootstrap-servers:localhost:9092}") String bootstrapServers
    ) {
        this.bootstrapServers = bootstrapServers;
    }

    @Bean
    @ConditionalOnMissingBean(name = "eventSchemaProducerFactory")
    public ProducerFactory<String, EventSchema> eventSchemaProducerFactory() {
        return new DefaultKafkaProducerFactory<>(
                producerProperties(),
                new StringSerializer(),
                EventSchemaKafkaSerdes.serializer()
        );
    }

    @Bean
    @ConditionalOnMissingBean(name = "eventSchemaKafkaTemplate")
    public KafkaTemplate<String, EventSchema> eventSchemaKafkaTemplate(
            ProducerFactory<String, EventSchema> eventSchemaProducerFactory
    ) {
        return new KafkaTemplate<>(eventSchemaProducerFactory);
    }

    private Map<String, Object> producerProperties() {
        Map<String, Object> properties = new HashMap<>();
        properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, EventSchemaKafkaSerdes.serializer().getClass());
        properties.put(ProducerConfig.ACKS_CONFIG, "all");
        properties.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        properties.put(ProducerConfig.LINGER_MS_CONFIG, 5);
        properties.put(ProducerConfig.BATCH_SIZE_CONFIG, 16_384);
        return properties;
    }
}
