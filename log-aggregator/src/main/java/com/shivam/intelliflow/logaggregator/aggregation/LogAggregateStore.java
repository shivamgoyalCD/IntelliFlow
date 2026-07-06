package com.shivam.intelliflow.logaggregator.aggregation;

import java.util.List;

public interface LogAggregateStore {
    void saveBatch(List<LogAggregateSnapshot> snapshots);
}
