package com.example.distributeddb;

import java.util.Comparator;

public record StoredValue(String key, String value, long version, boolean deleted) {
    static final Comparator<StoredValue> NEWEST_FIRST = Comparator.comparingLong(StoredValue::version).reversed();

    public StoredValue {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("key must not be blank");
        }
        if (!deleted && value == null) {
            throw new IllegalArgumentException("value is required for non-deleted records");
        }
        if (version < 0) {
            throw new IllegalArgumentException("version must not be negative");
        }
    }

    static StoredValue missing(String key) {
        return new StoredValue(key, null, 0L, true);
    }

    String toSyncLine() {
        return Codec.encode(key) + "\t" + version + "\t" + deleted + "\t" + Codec.encode(value == null ? "" : value);
    }

    static StoredValue fromSyncLine(String line) {
        String[] fields = line.split("\t", -1);
        if (fields.length != 4) {
            throw new IllegalArgumentException("invalid sync record");
        }
        String key = Codec.decode(fields[0]);
        long version = Long.parseLong(fields[1]);
        boolean deleted = Boolean.parseBoolean(fields[2]);
        String value = Codec.decode(fields[3]);
        return new StoredValue(key, deleted ? null : value, version, deleted);
    }
}
