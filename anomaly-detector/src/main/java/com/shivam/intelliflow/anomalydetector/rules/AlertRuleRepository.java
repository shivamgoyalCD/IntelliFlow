package com.shivam.intelliflow.anomalydetector.rules;

import java.util.List;

public interface AlertRuleRepository {
    List<AlertRule> findEnabledRules();
}
