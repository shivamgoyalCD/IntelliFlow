CREATE EXTENSION IF NOT EXISTS timescaledb;

CREATE TABLE IF NOT EXISTS log_event_minute_counts (
    window_start TIMESTAMPTZ NOT NULL,
    service_name TEXT NOT NULL,
    level TEXT NOT NULL,
    event_count BIGINT NOT NULL DEFAULT 0,
    distinct_trace_count BIGINT NOT NULL DEFAULT 0,
    message_length_min INTEGER NOT NULL DEFAULT 0,
    message_length_max INTEGER NOT NULL DEFAULT 0,
    message_length_total BIGINT NOT NULL DEFAULT 0,
    message_length_avg NUMERIC(12, 2) NOT NULL DEFAULT 0,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (window_start, service_name, level)
);

SELECT create_hypertable('log_event_minute_counts', 'window_start', if_not_exists => TRUE);

CREATE INDEX IF NOT EXISTS idx_log_event_minute_counts_service_window
    ON log_event_minute_counts (service_name, window_start DESC);

CREATE INDEX IF NOT EXISTS idx_log_event_minute_counts_level_window
    ON log_event_minute_counts (level, window_start DESC);
