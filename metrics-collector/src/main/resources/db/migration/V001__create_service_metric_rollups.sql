CREATE EXTENSION IF NOT EXISTS timescaledb;

CREATE TABLE IF NOT EXISTS service_metric_rollups (
    window_start TIMESTAMPTZ NOT NULL,
    service_name TEXT NOT NULL,
    event_count BIGINT NOT NULL DEFAULT 0,
    request_count BIGINT NOT NULL DEFAULT 0,
    error_count BIGINT NOT NULL DEFAULT 0,
    throughput_per_second NUMERIC(14, 4) NOT NULL DEFAULT 0,
    error_rate_percent NUMERIC(10, 4) NOT NULL DEFAULT 0,
    latency_p50_ms NUMERIC(14, 4),
    latency_p95_ms NUMERIC(14, 4),
    latency_p99_ms NUMERIC(14, 4),
    latency_sample_count BIGINT NOT NULL DEFAULT 0,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (window_start, service_name)
);

SELECT create_hypertable('service_metric_rollups', 'window_start', if_not_exists => TRUE);

CREATE INDEX IF NOT EXISTS idx_service_metric_rollups_service_window
    ON service_metric_rollups (service_name, window_start DESC);

CREATE INDEX IF NOT EXISTS idx_service_metric_rollups_window
    ON service_metric_rollups (window_start DESC);
