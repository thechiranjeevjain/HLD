package com.example.distributeddb;

import java.nio.file.Path;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

final class KeyValueStore {
    private final ConcurrentMap<String, StoredValue> records;
    private final AppendOnlyLog log;

    KeyValueStore(Path logFile) {
        this.log = new AppendOnlyLog(logFile);
        this.records = new ConcurrentHashMap<>(log.load());
    }

    synchronized boolean apply(StoredValue incoming) {
        StoredValue current = records.get(incoming.key());
        if (current != null && current.version() >= incoming.version()) {
            return false;
        }
        records.put(incoming.key(), incoming);
        log.append(incoming);
        return true;
    }

    Optional<StoredValue> get(String key) {
        return Optional.ofNullable(records.get(key));
    }

    long maxVersion() {
        return records.values().stream()
                .map(StoredValue::version)
                .max(Comparator.naturalOrder())
                .orElse(0L);
    }

    Collection<StoredValue> allRecords() {
        return List.copyOf(records.values());
    }
}
