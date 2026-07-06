package com.shivam.intelliflow.anomalydetector.detector;

import com.shivam.intelliflow.anomalydetector.model.MetricSample;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class RedisMetricHistoryStore implements MetricHistoryStore {
    private static final String MEMBER_SEPARATOR = "|";

    private final StringRedisTemplate redisTemplate;
    private final String keyPrefix;

    public RedisMetricHistoryStore(
            StringRedisTemplate redisTemplate,
            @Value("${intelliflow.anomaly-detector.redis.metric-history-prefix:intelliflow:metrics}") String keyPrefix
    ) {
        this.redisTemplate = redisTemplate;
        this.keyPrefix = keyPrefix;
    }

    @Override
    public List<BigDecimal> recentValues(MetricSample sample, Duration historyWindow) {
        Instant now = sample.timestamp();
        Instant cutoff = now.minus(historyWindow);
        Set<String> members = redisTemplate.opsForZSet()
                .rangeByScore(historyKey(sample), cutoff.toEpochMilli(), now.toEpochMilli());

        if (members == null || members.isEmpty()) {
            return List.of();
        }

        return members.stream()
                .map(this::valueFromMember)
                .flatMap(java.util.Optional::stream)
                .toList();
    }

    @Override
    public void record(MetricSample sample, Duration retention) {
        String key = historyKey(sample);
        long score = sample.timestamp().toEpochMilli();
        redisTemplate.opsForZSet().add(key, member(sample, score), score);
        redisTemplate.opsForZSet().removeRangeByScore(key, 0, sample.timestamp().minus(retention).toEpochMilli());
        redisTemplate.expire(key, retention.plus(Duration.ofMinutes(5)));
    }

    private String historyKey(MetricSample sample) {
        return keyPrefix + ":" + normalize(sample.serviceName()) + ":" + normalize(sample.metricName());
    }

    private String member(MetricSample sample, long score) {
        return score + MEMBER_SEPARATOR + UUID.randomUUID() + MEMBER_SEPARATOR + sample.value().toPlainString();
    }

    private java.util.Optional<BigDecimal> valueFromMember(String member) {
        int separatorIndex = member.lastIndexOf(MEMBER_SEPARATOR);
        if (separatorIndex < 0 || separatorIndex == member.length() - 1) {
            return java.util.Optional.empty();
        }

        try {
            return java.util.Optional.of(new BigDecimal(member.substring(separatorIndex + 1)));
        } catch (NumberFormatException exception) {
            return java.util.Optional.empty();
        }
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return "unknown";
        }
        return value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_.-]", "_");
    }
}
