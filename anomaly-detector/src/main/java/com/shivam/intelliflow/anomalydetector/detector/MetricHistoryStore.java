package com.shivam.intelliflow.anomalydetector.detector;

import com.shivam.intelliflow.anomalydetector.model.MetricSample;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;

public interface MetricHistoryStore {
    List<BigDecimal> recentValues(MetricSample sample, Duration historyWindow);

    void record(MetricSample sample, Duration retention);
}
