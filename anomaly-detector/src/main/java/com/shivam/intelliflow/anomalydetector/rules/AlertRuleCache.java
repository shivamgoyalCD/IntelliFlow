package com.shivam.intelliflow.anomalydetector.rules;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class AlertRuleCache {
    private static final Logger log = LoggerFactory.getLogger(AlertRuleCache.class);

    private final AlertRuleRepository alertRuleRepository;
    private volatile List<AlertRule> enabledRules = List.of();

    public AlertRuleCache(AlertRuleRepository alertRuleRepository) {
        this.alertRuleRepository = alertRuleRepository;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void loadOnStartup() {
        refreshRules();
    }

    @Scheduled(fixedDelayString = "${intelliflow.anomaly-detector.rules.refresh-interval-ms:60000}")
    public void refreshRules() {
        List<AlertRule> rules = List.copyOf(alertRuleRepository.findEnabledRules());
        enabledRules = rules;
        log.info("Loaded {} enabled alert rule(s)", rules.size());
    }

    public List<AlertRule> rulesFor(String serviceName) {
        return enabledRules.stream()
                .filter(rule -> rule.appliesTo(serviceName))
                .toList();
    }
}
