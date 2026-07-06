package com.shivam.intelliflow.metricscollector.config;

import com.shivam.intelliflow.common.config.KafkaConsumerConfig;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration(proxyBeanMethods = false)
@Import(KafkaConsumerConfig.class)
public class MetricsCollectorKafkaConfig {
}
