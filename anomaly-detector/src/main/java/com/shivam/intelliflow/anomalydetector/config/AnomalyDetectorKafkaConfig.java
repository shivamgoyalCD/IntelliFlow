package com.shivam.intelliflow.anomalydetector.config;

import com.shivam.intelliflow.common.config.KafkaConsumerConfig;
import com.shivam.intelliflow.common.config.KafkaProducerConfig;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration(proxyBeanMethods = false)
@Import({KafkaConsumerConfig.class, KafkaProducerConfig.class})
public class AnomalyDetectorKafkaConfig {
}
