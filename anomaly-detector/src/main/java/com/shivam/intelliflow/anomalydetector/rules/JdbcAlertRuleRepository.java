package com.shivam.intelliflow.anomalydetector.rules;

import com.shivam.intelliflow.anomalydetector.model.AlertSeverity;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcAlertRuleRepository implements AlertRuleRepository {
    private static final String SELECT_ENABLED_RULES = """
            SELECT id,
                   name,
                   service_name,
                   rule_type,
                   threshold_value,
                   evaluation_window_seconds,
                   consecutive_count,
                   cooldown_seconds,
                   severity,
                   enabled
            FROM alert_rules
            WHERE enabled = true
            ORDER BY service_name, rule_type, name
            """;

    private final JdbcTemplate jdbcTemplate;

    public JdbcAlertRuleRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<AlertRule> findEnabledRules() {
        return jdbcTemplate.query(SELECT_ENABLED_RULES, this::mapRule);
    }

    private AlertRule mapRule(ResultSet resultSet, int rowNumber) throws SQLException {
        return new AlertRule(
                UUID.fromString(resultSet.getString("id")),
                resultSet.getString("name"),
                resultSet.getString("service_name"),
                AlertRuleType.valueOf(resultSet.getString("rule_type")),
                resultSet.getBigDecimal("threshold_value"),
                resultSet.getInt("evaluation_window_seconds"),
                resultSet.getInt("consecutive_count"),
                resultSet.getInt("cooldown_seconds"),
                AlertSeverity.valueOf(resultSet.getString("severity")),
                resultSet.getBoolean("enabled")
        );
    }
}
