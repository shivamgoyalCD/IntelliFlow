package com.shivam.intelliflow.auth.config;

import com.shivam.intelliflow.common.config.KafkaProducerConfig;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration(proxyBeanMethods = false)
@Import(KafkaProducerConfig.class)
public class AuthServiceKafkaConfig {
}
