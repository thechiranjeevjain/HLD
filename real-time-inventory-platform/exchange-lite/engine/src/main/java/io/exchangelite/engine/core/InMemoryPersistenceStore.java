package io.exchangelite.engine.core;

import io.exchangelite.common.domain.ExecutionReport;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

public final class InMemoryPersistenceStore implements PersistenceStore {
    private final int capacity;
    private final ArrayDeque<ExecutionReport> reports = new ArrayDeque<>();

    public InMemoryPersistenceStore(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("capacity must be positive");
        }
        this.capacity = capacity;
    }

    @Override
    public synchronized void append(ExecutionReport report) {
        if (reports.size() == capacity) {
            reports.removeFirst();
        }
        reports.addLast(report);
    }

    @Override
    public synchronized List<ExecutionReport> recentReports() {
        return List.copyOf(new ArrayList<>(reports));
    }
}
