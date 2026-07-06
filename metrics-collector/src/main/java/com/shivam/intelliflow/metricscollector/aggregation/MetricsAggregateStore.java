package com.shivam.intelliflow.metricscollector.aggregation;

import com.shivam.intelliflow.metricscollector.model.ServiceMetricSnapshot;
import java.util.List;

public interface MetricsAggregateStore {
    void saveBatch(List<ServiceMetricSnapshot> snapshots);
}
