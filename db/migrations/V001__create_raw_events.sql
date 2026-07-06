CREATE EXTENSION IF NOT EXISTS timescaledb;

CREATE TABLE IF NOT EXISTS raw_events (
    id BIGSERIAL,
    event_id TEXT NOT NULL,
    event_timestamp TIMESTAMPTZ NOT NULL,
    service_name TEXT NOT NULL,
    level TEXT NOT NULL,
    trace_id TEXT,
    span_id TEXT,
    message TEXT,
    metadata JSONB NOT NULL DEFAULT '{}'::jsonb,
    raw_event JSONB NOT NULL,
    received_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (event_timestamp, event_id)
);

SELECT create_hypertable('raw_events', 'event_timestamp', if_not_exists => TRUE);

CREATE INDEX IF NOT EXISTS idx_raw_events_event_timestamp
    ON raw_events (event_timestamp DESC);

CREATE INDEX IF NOT EXISTS idx_raw_events_service_timestamp
    ON raw_events (service_name, event_timestamp DESC);

CREATE INDEX IF NOT EXISTS idx_raw_events_level_timestamp
    ON raw_events (level, event_timestamp DESC);

CREATE INDEX IF NOT EXISTS idx_raw_events_trace_id
    ON raw_events (trace_id)
    WHERE trace_id IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_raw_events_metadata_gin
    ON raw_events USING GIN (metadata jsonb_path_ops);
