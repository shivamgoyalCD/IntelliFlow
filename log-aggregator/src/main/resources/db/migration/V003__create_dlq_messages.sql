CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE IF NOT EXISTS dlq_messages (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    dlq_event_id TEXT,
    origin_service TEXT,
    original_topic TEXT NOT NULL,
    original_partition INTEGER,
    original_offset BIGINT,
    message_key TEXT,
    payload JSONB NOT NULL DEFAULT '{}'::jsonb,
    payload_raw TEXT,
    error_class TEXT,
    error_message TEXT,
    failure_count INTEGER NOT NULL DEFAULT 1,
    status TEXT NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'REPLAYED', 'DISCARDED')),
    failed_at TIMESTAMPTZ,
    replay_topic TEXT,
    replayed_at TIMESTAMPTZ,
    last_error TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_dlq_messages_status
    ON dlq_messages (status);

CREATE INDEX IF NOT EXISTS idx_dlq_messages_original_topic
    ON dlq_messages (original_topic);

CREATE INDEX IF NOT EXISTS idx_dlq_messages_created_at
    ON dlq_messages (created_at DESC);
