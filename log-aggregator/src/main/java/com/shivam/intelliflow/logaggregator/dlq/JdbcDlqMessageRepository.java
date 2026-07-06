package com.shivam.intelliflow.logaggregator.dlq;

import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcDlqMessageRepository implements DlqMessageRepository {
    private static final String INSERT_SQL = """
            INSERT INTO dlq_messages (
                dlq_event_id,
                origin_service,
                original_topic,
                original_partition,
                original_offset,
                message_key,
                payload,
                payload_raw,
                error_class,
                error_message,
                failure_count,
                status,
                failed_at
            )
            VALUES (?, ?, ?, ?, ?, ?, ?::jsonb, ?, ?, ?, ?, ?, ?)
            RETURNING id
            """;

    private static final String SELECT_BASE = """
            SELECT id, dlq_event_id, origin_service, original_topic, original_partition,
                   original_offset, message_key, payload::text AS payload, payload_raw,
                   error_class, error_message, failure_count, status, failed_at,
                   replay_topic, replayed_at, last_error, created_at, updated_at
            FROM dlq_messages
            """;

    private final JdbcTemplate jdbcTemplate;

    public JdbcDlqMessageRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public UUID save(DlqMessage message) {
        return jdbcTemplate.queryForObject(
                INSERT_SQL,
                UUID.class,
                message.dlqEventId(),
                message.originService(),
                message.originalTopic(),
                message.originalPartition(),
                message.originalOffset(),
                message.messageKey(),
                message.payload() == null ? "{}" : message.payload(),
                message.payloadRaw(),
                message.errorClass(),
                message.errorMessage(),
                message.failureCount(),
                message.status().name(),
                timestamp(message.failedAt())
        );
    }

    @Override
    public Optional<DlqMessage> findById(UUID id) {
        List<DlqMessage> results = jdbcTemplate.query(
                SELECT_BASE + " WHERE id = ?",
                rowMapper(),
                id
        );
        return results.stream().findFirst();
    }

    @Override
    public List<DlqMessage> findByStatus(DlqMessageStatus status, int limit) {
        return jdbcTemplate.query(
                SELECT_BASE + " WHERE status = ? ORDER BY created_at DESC LIMIT ?",
                rowMapper(),
                status.name(),
                limit
        );
    }

    @Override
    public List<DlqMessage> findRecent(int limit) {
        return jdbcTemplate.query(
                SELECT_BASE + " ORDER BY created_at DESC LIMIT ?",
                rowMapper(),
                limit
        );
    }

    @Override
    public void markReplayed(UUID id, String replayTopic) {
        jdbcTemplate.update(
                """
                UPDATE dlq_messages
                SET status = 'REPLAYED', replay_topic = ?, replayed_at = now(), last_error = NULL, updated_at = now()
                WHERE id = ?
                """,
                replayTopic,
                id
        );
    }

    @Override
    public void markReplayFailed(UUID id, String error) {
        jdbcTemplate.update(
                """
                UPDATE dlq_messages
                SET last_error = ?, updated_at = now()
                WHERE id = ?
                """,
                error,
                id
        );
    }

    private RowMapper<DlqMessage> rowMapper() {
        return (ResultSet rs, int rowNum) -> new DlqMessage(
                rs.getObject("id", UUID.class),
                rs.getString("dlq_event_id"),
                rs.getString("origin_service"),
                rs.getString("original_topic"),
                (Integer) rs.getObject("original_partition"),
                (Long) rs.getObject("original_offset"),
                rs.getString("message_key"),
                rs.getString("payload"),
                rs.getString("payload_raw"),
                rs.getString("error_class"),
                rs.getString("error_message"),
                rs.getInt("failure_count"),
                DlqMessageStatus.valueOf(rs.getString("status")),
                instant(rs.getTimestamp("failed_at")),
                rs.getString("replay_topic"),
                instant(rs.getTimestamp("replayed_at")),
                rs.getString("last_error"),
                instant(rs.getTimestamp("created_at")),
                instant(rs.getTimestamp("updated_at"))
        );
    }

    private static Timestamp timestamp(Instant instant) {
        return instant == null ? null : Timestamp.from(instant);
    }

    private static Instant instant(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toInstant();
    }
}
