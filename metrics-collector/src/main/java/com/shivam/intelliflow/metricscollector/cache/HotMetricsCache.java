package com.shivam.intelliflow.metricscollector.cache;

import com.shivam.intelliflow.metricscollector.model.ServiceMetricSnapshot;
import java.util.List;

public interface HotMetricsCache {
    void cacheAll(List<ServiceMetricSnapshot> snapshots);
}
