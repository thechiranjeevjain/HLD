package io.exchangelite.engine.core;

import io.exchangelite.common.domain.ExecutionReport;

import java.util.List;

public interface PersistenceStore {
    void append(ExecutionReport report);

    List<ExecutionReport> recentReports();
}
