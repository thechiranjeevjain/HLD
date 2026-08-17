package com.example.distributeddb;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.Map;

final class AppendOnlyLog {
    private final Path logFile;

    AppendOnlyLog(Path logFile) {
        this.logFile = logFile;
    }

    Map<String, StoredValue> load() {
        Map<String, StoredValue> loaded = new HashMap<>();
        if (!Files.exists(logFile)) {
            return loaded;
        }
        try {
            for (String line : Files.readAllLines(logFile, StandardCharsets.UTF_8)) {
                if (line.isBlank()) {
                    continue;
                }
                String[] fields = line.split("\t", -1);
                if (fields.length != 4) {
                    throw new IllegalStateException("Invalid WAL line in " + logFile + ": " + line);
                }
                long version = Long.parseLong(fields[0]);
                boolean deleted = fields[1].equals("D");
                String key = Codec.decode(fields[2]);
                String value = Codec.decode(fields[3]);
                StoredValue candidate = new StoredValue(key, deleted ? null : value, version, deleted);
                StoredValue current = loaded.get(key);
                if (current == null || candidate.version() > current.version()) {
                    loaded.put(key, candidate);
                }
            }
            return loaded;
        } catch (IOException ex) {
            throw new UncheckedIOException("Could not read WAL " + logFile, ex);
        }
    }

    synchronized void append(StoredValue value) {
        try {
            Files.createDirectories(logFile.getParent());
            String line = value.version()
                    + "\t" + (value.deleted() ? "D" : "P")
                    + "\t" + Codec.encode(value.key())
                    + "\t" + Codec.encode(value.value() == null ? "" : value.value())
                    + System.lineSeparator();
            Files.writeString(logFile, line, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException ex) {
            throw new UncheckedIOException("Could not append WAL " + logFile, ex);
        }
    }
}
