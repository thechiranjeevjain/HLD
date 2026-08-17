# Mini KV Storage Engine Demo Script

## Run

```powershell
cd G:\TechStudyNotes\SystemDesignProjects\mini-kv-storage-engine
mvn clean package
java -jar target/mini-kv-storage-engine-0.1.0-SNAPSHOT.jar
```

## Explain While It Runs

1. Writes append to `wal.log` first.
2. Memory state and cache update only after the log append.
3. Key `c` expires after its TTL.
4. Compaction rewrites the WAL to only current live records.
5. The cleanup thread is shut down explicitly.

## Interview Close

Say: this project is about invariants. Durability depends on WAL ordering; read correctness depends on expiry checks; performance optimizations must not become the source of truth.
