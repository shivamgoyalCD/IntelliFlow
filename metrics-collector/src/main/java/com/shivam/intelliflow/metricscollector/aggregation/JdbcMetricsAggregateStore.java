package com.shivam.intelliflow.metricscollector.aggregation;

import com.shivam.intelliflow.metricscollector.model.ServiceMetricSnapshot;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class JdbcMetricsAggregateStore implements MetricsAggregateStore {
    private static final String INSERT_SQL = """
            INSERT INTO service_metric_rollups (
                window_start,
                service_name,
                event_count,
                request_count,
                error_count,
                throughput_per_second,
                error_rate_percent,
                latency_p50_ms,
                latency_p95_ms,
                latency_p99_ms,
                latency_sample_count,
                updated_at
            )
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, now())
            ON CONFLICT (window_start, service_name)
            DO UPDATE SET
                event_count = service_metric_rollups.event_count + EXCLUDED.event_count,
                request_count = service_metric_rollups.request_count + EXCLUDED.request_count,
                error_count = service_metric_rollups.error_count + EXCLUDED.error_count,
                throughput_per_second = EXCLUDED.throughput_per_second,
                error_rate_percent = CASE
                    WHEN (service_metric_rollups.request_count + EXCLUDED.request_count) = 0 THEN 0
                    ELSE ROUND(
                        ((service_metric_rollups.error_count + EXCLUDED.error_count)::numeric * 100)
                        / (service_metric_rollups.request_count + EXCLUDED.request_count),
                        4
                    )
                END,
                latency_p50_ms = EXCLUDED.latency_p50_ms,
                latency_p95_ms = EXCLUDED.latency_p95_ms,
                latency_p99_ms = EXCLUDED.latency_p99_ms,
                latency_sample_count = service_metric_rollups.latency_sample_count + EXCLUDED.latency_sample_count,
                updated_at = now()
            """;

    private final JdbcTemplate jdbcTemplate;

    public JdbcMetricsAggregateStore(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    @Transactional
    public void saveBatch(List<ServiceMetricSnapshot> snapshots) {
        if (snapshots.isEmpty()) {
            return;
        }
        jdbcTemplate.batchUpdate(INSERT_SQL, snapshots, snapshots.size(), this::bindSnapshot);
    }

    private void bindSnapshot(PreparedStatement statement, ServiceMetricSnapshot snapshot) throws SQLException {
        statement.setTimestamp(1, Timestamp.from(snapshot.windowStart()));
        statement.setString(2, snapshot.serviceName());
        statement.setLong(3, snapshot.eventCount());
        statement.setLong(4, snapshot.requestCount());
        statement.setLong(5, snapshot.errorCount());
        statement.setBigDecimal(6, snapshot.throughputPerSecond());
        statement.setBigDecimal(7, snapshot.errorRatePercent());
        statement.setBigDecimal(8, snapshot.latencyP50Ms());
        statement.setBigDecimal(9, snapshot.latencyP95Ms());
        statement.setBigDecimal(10, snapshot.latencyP99Ms());
        statement.setLong(11, snapshot.latencySampleCount());
    }
}
