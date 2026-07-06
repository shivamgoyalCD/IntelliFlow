CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE IF NOT EXISTS incidents (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    service_name TEXT NOT NULL,
    alert_rule_id UUID REFERENCES alert_rules (id) ON DELETE SET NULL,
    dedup_key TEXT,
    title TEXT NOT NULL,
    description TEXT,
    severity TEXT NOT NULL DEFAULT 'WARN' CHECK (severity IN ('INFO', 'WARN', 'ERROR', 'FATAL')),
    status TEXT NOT NULL DEFAULT 'OPEN' CHECK (status IN ('OPEN', 'ACKNOWLEDGED', 'RESOLVED')),
    occurrence_count BIGINT NOT NULL DEFAULT 1,
    first_seen_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    last_seen_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    acknowledged_at TIMESTAMPTZ,
    resolved_at TIMESTAMPTZ,
    metadata JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_incidents_service_status
    ON incidents (service_name, status);

CREATE INDEX IF NOT EXISTS idx_incidents_status
    ON incidents (status);

CREATE UNIQUE INDEX IF NOT EXISTS idx_incidents_open_dedup_key
    ON incidents (dedup_key)
    WHERE dedup_key IS NOT NULL AND status <> 'RESOLVED';
