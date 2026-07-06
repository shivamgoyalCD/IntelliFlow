package com.shivam.intelliflow.logaggregator.persistence;

import com.shivam.intelliflow.common.event.EventSchema;
import com.shivam.intelliflow.common.util.JsonUtils;
import com.shivam.intelliflow.logaggregator.model.LogEventBatch;
import com.shivam.intelliflow.logaggregator.model.LogEventEnvelope;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class JdbcLogEventStore implements LogEventStore {
    private static final String INSERT_SQL = """
            INSERT INTO raw_events (
                event_id,
                event_timestamp,
                service_name,
                level,
                trace_id,
                span_id,
                message,
                metadata,
                raw_event,
                received_at
            )
            VALUES (?, ?, ?, ?, ?, ?, ?, ?::jsonb, ?::jsonb, ?)
            ON CONFLICT (event_timestamp, event_id) DO NOTHING
            """;

    private final JdbcTemplate jdbcTemplate;

    public JdbcLogEventStore(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    @Transactional
    public void saveBatch(LogEventBatch batch) {
        if (batch.isEmpty()) {
            return;
        }

        jdbcTemplate.batchUpdate(INSERT_SQL, batch.events(), batch.size(), this::bindEvent);
    }

    private void bindEvent(PreparedStatement statement, LogEventEnvelope envelope) throws SQLException {
        EventSchema event = envelope.event();

        statement.setString(1, event.eventId());
        statement.setTimestamp(2, timestamp(event.timestamp()));
        statement.setString(3, event.serviceName());
        statement.setString(4, event.level().name());
        statement.setString(5, event.traceId());
        statement.setString(6, event.spanId());
        statement.setString(7, event.message());
        statement.setString(8, JsonUtils.toJson(event.metadata()));
        statement.setString(9, JsonUtils.toJson(event));
        statement.setTimestamp(10, timestamp(envelope.receivedAt()));
    }

    private Timestamp timestamp(Instant instant) {
        return Timestamp.from(instant);
    }
}
