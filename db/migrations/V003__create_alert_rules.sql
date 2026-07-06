CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE IF NOT EXISTS alert_rules (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name TEXT NOT NULL UNIQUE,
    service_name TEXT NOT NULL DEFAULT '*',
    rule_type TEXT NOT NULL CHECK (
        rule_type IN ('ERROR_RATE', 'LATENCY_P99', 'THROUGHPUT_DROP', 'CONSECUTIVE_ERRORS')
    ),
    threshold_value NUMERIC(14, 4) NOT NULL,
    evaluation_window_seconds INTEGER NOT NULL DEFAULT 60,
    consecutive_count INTEGER NOT NULL DEFAULT 1,
    cooldown_seconds INTEGER NOT NULL DEFAULT 60,
    severity TEXT NOT NULL DEFAULT 'WARN' CHECK (severity IN ('INFO', 'WARN', 'ERROR', 'FATAL')),
    enabled BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_alert_rules_enabled
    ON alert_rules (enabled);

CREATE INDEX IF NOT EXISTS idx_alert_rules_service_type
    ON alert_rules (service_name, rule_type);

INSERT INTO alert_rules (
    id,
    name,
    service_name,
    rule_type,
    threshold_value,
    evaluation_window_seconds,
    consecutive_count,
    cooldown_seconds,
    severity
) VALUES
    ('10000000-0000-0000-0000-000000000001', 'default-error-rate-high', '*', 'ERROR_RATE', 5.0000, 60, 1, 60, 'ERROR'),
    ('10000000-0000-0000-0000-000000000002', 'default-latency-p99-high', '*', 'LATENCY_P99', 1000.0000, 60, 1, 60, 'WARN'),
    ('10000000-0000-0000-0000-000000000003', 'default-throughput-drop', '*', 'THROUGHPUT_DROP', 1.0000, 60, 1, 60, 'WARN'),
    ('10000000-0000-0000-0000-000000000004', 'default-consecutive-errors', '*', 'CONSECUTIVE_ERRORS', 3.0000, 60, 3, 60, 'ERROR')
ON CONFLICT (id) DO NOTHING;
