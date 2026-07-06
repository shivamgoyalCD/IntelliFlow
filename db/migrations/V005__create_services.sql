CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE IF NOT EXISTS services (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name TEXT NOT NULL UNIQUE,
    display_name TEXT,
    description TEXT,
    owner_team TEXT,
    environment TEXT NOT NULL DEFAULT 'local',
    enabled BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_services_enabled
    ON services (enabled);

INSERT INTO services (name, display_name, description, owner_team) VALUES
    ('order-service', 'Order Service', 'Handles order lifecycle and payment orchestration', 'platform'),
    ('payment-service', 'Payment Service', 'Processes payments for orders', 'platform'),
    ('auth-service', 'Auth Service', 'Issues and validates JWT credentials', 'platform'),
    ('log-aggregator', 'Log Aggregator', 'Persists and indexes log events, owns the DLQ flow', 'observability'),
    ('anomaly-detector', 'Anomaly Detector', 'Evaluates alert rules against metric streams', 'observability'),
    ('metrics-collector', 'Metrics Collector', 'Aggregates service metric rollups', 'observability')
ON CONFLICT (name) DO NOTHING;
