package com.shivam.intelliflow.metricscollector.cache;

import com.shivam.intelliflow.common.util.JsonUtils;
import com.shivam.intelliflow.metricscollector.model.ServiceMetricSnapshot;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class RedisHotMetricsCache implements HotMetricsCache {
    private final StringRedisTemplate redisTemplate;
    private final String keyPrefix;
    private final Duration ttl;

    public RedisHotMetricsCache(
            StringRedisTemplate redisTemplate,
            @Value("${intelliflow.metrics-collector.cache.key-prefix:intelliflow:hot-metrics}") String keyPrefix,
            @Value("${intelliflow.metrics-collector.cache.ttl-seconds:120}") long ttlSeconds
    ) {
        this.redisTemplate = redisTemplate;
        this.keyPrefix = keyPrefix;
        this.ttl = Duration.ofSeconds(Math.max(1, ttlSeconds));
    }

    @Override
    public void cacheAll(List<ServiceMetricSnapshot> snapshots) {
        for (ServiceMetricSnapshot snapshot : snapshots) {
            redisTemplate.opsForValue().set(cacheKey(snapshot.serviceName()), JsonUtils.toJson(snapshot), ttl);
        }
    }

    private String cacheKey(String serviceName) {
        return keyPrefix + ":" + normalize(serviceName);
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return "unknown";
        }
        return value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_.-]", "_");
    }
}
