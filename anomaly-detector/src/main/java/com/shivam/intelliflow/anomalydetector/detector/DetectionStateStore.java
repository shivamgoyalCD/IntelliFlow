package com.shivam.intelliflow.anomalydetector.detector;

import com.shivam.intelliflow.anomalydetector.rules.AlertRule;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
public class DetectionStateStore {
    private final Map<String, Integer> consecutiveErrorCounts = new ConcurrentHashMap<>();
    private final Map<UUID, Instant> lastAlertTimes = new ConcurrentHashMap<>();

    public int recordErrorState(String serviceName, AlertRule rule, boolean error) {
        String key = serviceName + ":" + rule.id();
        if (!error) {
            consecutiveErrorCounts.remove(key);
            return 0;
        }
        return consecutiveErrorCounts.merge(key, 1, Integer::sum);
    }

    public boolean canPublish(AlertRule rule, Instant now) {
        Instant lastAlertTime = lastAlertTimes.get(rule.id());
        if (lastAlertTime == null) {
            return true;
        }
        return Duration.between(lastAlertTime, now).getSeconds() >= rule.cooldownSeconds();
    }

    public void markPublished(AlertRule rule, Instant now) {
        lastAlertTimes.put(rule.id(), now);
    }
}
