# Mini KV Storage Engine

A Java 17 storage-engine lab that implements a single-node key-value store with a write-ahead log, in-memory state, TTL, LRU cache, recovery, and WAL compaction.

## What It Shows

- WAL append before memory mutation.
- Recovery by replaying accepted records.
- TTL expiry on reads and periodic cleanup.
- Delete tombstones.
- LRU cache as an optimization, not source of truth.
- Coarse locking across WAL, store, and cache updates.

## Run

```powershell
cd G:\TechStudyNotes\SystemDesignProjects\mini-kv-storage-engine
mvn clean package
java -jar target/mini-kv-storage-engine-0.1.0-SNAPSHOT.jar
```

## Expected Demo Behavior

The demo writes keys, reads them, waits for TTL expiry, compacts the WAL, and shuts down the background cleaner.

## Learning Docs

- [Interview Guide](docs/INTERVIEW_GUIDE.md)
- [Diagrams](docs/DIAGRAMS.md)
- [Demo Script](docs/DEMO_SCRIPT.md)
- [Interviewer Grilling](docs/APPENDIX_1_INTERVIEWER_GRILLING.md)
- [Tradeoffs](docs/APPENDIX_2_TRADEOFFS.md)
- [Real World Mapping](docs/APPENDIX_3_REAL_WORLD_MAPPING.md)
- [Teaching Analogies](docs/APPENDIX_4_TEACHING_ANALOGIES.md)
