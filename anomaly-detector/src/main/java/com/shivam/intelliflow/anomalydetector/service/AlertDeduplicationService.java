package com.shivam.intelliflow.anomalydetector.service;

import com.shivam.intelliflow.anomalydetector.model.AlertDeduplicationKey;
import com.shivam.intelliflow.anomalydetector.model.AlertEvent;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class AlertDeduplicationService {
    private final StringRedisTemplate redisTemplate;
    private final String keyPrefix;
    private final Duration cooldown;

    public AlertDeduplicationService(
            StringRedisTemplate redisTemplate,
            @Value("${intelliflow.anomaly-detector.deduplication.key-prefix:intelliflow:alert-dedup}") String keyPrefix,
            @Value("${intelliflow.anomaly-detector.deduplication.cooldown-seconds:300}") long cooldownSeconds
    ) {
        this.redisTemplate = redisTemplate;
        this.keyPrefix = keyPrefix;
        this.cooldown = Duration.ofSeconds(Math.max(1, cooldownSeconds));
    }

    public boolean shouldPublish(AlertEvent alertEvent) {
        String key = AlertDeduplicationKey.from(alertEvent).redisKey(keyPrefix);
        Boolean created = redisTemplate.opsForValue().setIfAbsent(key, alertEvent.alertId().toString(), cooldown);
        return Boolean.TRUE.equals(created);
    }
}
