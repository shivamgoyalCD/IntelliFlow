package com.shivam.intelliflow.logaggregator.aggregation;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class JdbcLogAggregateStore implements LogAggregateStore {
    private static final String INSERT_SQL = """
            INSERT INTO log_event_minute_counts (
                window_start,
                service_name,
                level,
                event_count,
                distinct_trace_count,
                message_length_min,
                message_length_max,
                message_length_total,
                message_length_avg,
                updated_at
            )
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, now())
            ON CONFLICT (window_start, service_name, level)
            DO UPDATE SET
                event_count = log_event_minute_counts.event_count + EXCLUDED.event_count,
                distinct_trace_count = log_event_minute_counts.distinct_trace_count + EXCLUDED.distinct_trace_count,
                message_length_min = LEAST(log_event_minute_counts.message_length_min, EXCLUDED.message_length_min),
                message_length_max = GREATEST(log_event_minute_counts.message_length_max, EXCLUDED.message_length_max),
                message_length_total = log_event_minute_counts.message_length_total + EXCLUDED.message_length_total,
                message_length_avg = ROUND(
                    (log_event_minute_counts.message_length_total + EXCLUDED.message_length_total)::numeric
                    / NULLIF(log_event_minute_counts.event_count + EXCLUDED.event_count, 0),
                    2
                ),
                updated_at = now()
            """;

    private final JdbcTemplate jdbcTemplate;

    public JdbcLogAggregateStore(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    @Transactional
    public void saveBatch(List<LogAggregateSnapshot> snapshots) {
        if (snapshots.isEmpty()) {
            return;
        }

        jdbcTemplate.batchUpdate(INSERT_SQL, snapshots, snapshots.size(), this::bindAggregate);
    }

    private void bindAggregate(PreparedStatement statement, LogAggregateSnapshot snapshot) throws SQLException {
        statement.setTimestamp(1, Timestamp.from(snapshot.windowStart()));
        statement.setString(2, snapshot.serviceName());
        statement.setString(3, snapshot.level());
        statement.setLong(4, snapshot.eventCount());
        statement.setLong(5, snapshot.distinctTraceCount());
        statement.setInt(6, snapshot.messageLengthMin());
        statement.setInt(7, snapshot.messageLengthMax());
        statement.setLong(8, snapshot.messageLengthTotal());
        statement.setBigDecimal(9, BigDecimal.valueOf(snapshot.messageLengthAverage()).setScale(2, RoundingMode.HALF_UP));
    }
}
