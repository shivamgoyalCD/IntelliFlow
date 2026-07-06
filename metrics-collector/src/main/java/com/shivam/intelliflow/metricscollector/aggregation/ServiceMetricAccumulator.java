package com.shivam.intelliflow.metricscollector.aggregation;

import com.shivam.intelliflow.metricscollector.model.MetricEvent;
import com.shivam.intelliflow.metricscollector.model.MetricWindowKey;
import com.shivam.intelliflow.metricscollector.model.ServiceMetricSnapshot;
import com.tdunning.math.stats.TDigest;
import java.math.BigDecimal;
import java.math.RoundingMode;

public class ServiceMetricAccumulator {
    private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);
    private static final BigDecimal WINDOW_SECONDS = BigDecimal.valueOf(60);

    private final TDigest latencyDigest;
    private long eventCount;
    private long requestCount;
    private long errorCount;
    private long latencySampleCount;

    public ServiceMetricAccumulator(double compression) {
        this.latencyDigest = TDigest.createMergingDigest(compression);
    }

    public void record(MetricEvent event) {
        eventCount++;
        requestCount += Math.max(0, event.requestCount());
        errorCount += Math.max(0, event.errorCount());

        if (event.latencyMs() != null && event.latencyMs().signum() >= 0) {
            latencyDigest.add(event.latencyMs().doubleValue());
            latencySampleCount++;
        }
    }

    public ServiceMetricSnapshot snapshot(MetricWindowKey key) {
        return new ServiceMetricSnapshot(
                key.windowStart(),
                key.serviceName(),
                eventCount,
                requestCount,
                errorCount,
                decimal(requestCount).divide(WINDOW_SECONDS, 4, RoundingMode.HALF_UP),
                errorRate(),
                percentile(0.50),
                percentile(0.95),
                percentile(0.99),
                latencySampleCount
        );
    }

    private BigDecimal errorRate() {
        if (requestCount == 0) {
            return BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
        }
        return decimal(errorCount)
                .multiply(ONE_HUNDRED)
                .divide(decimal(requestCount), 4, RoundingMode.HALF_UP);
    }

    private BigDecimal percentile(double quantile) {
        if (latencySampleCount == 0) {
            return null;
        }
        return BigDecimal.valueOf(latencyDigest.quantile(quantile)).setScale(4, RoundingMode.HALF_UP);
    }

    private BigDecimal decimal(long value) {
        return BigDecimal.valueOf(value);
    }
}
