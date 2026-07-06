package com.shivam.intelliflow.metricscollector;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class MetricsCollectorApplication {
    public static void main(String[] args) {
        SpringApplication.run(MetricsCollectorApplication.class, args);
    }
}
