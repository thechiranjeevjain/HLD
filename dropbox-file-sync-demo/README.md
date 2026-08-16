# Dropbox File Sync — Runnable Interview Demo

A Java 17 and Spring Boot implementation of the important Dropbox sync invariants. It is deliberately small enough to explain in an interview while exercising real chunk transfer, version commits, conflicts, retries, persistence, downloads, rename/delete, and ordered change cursors.

## Run it

Requires Java 17 and Maven.

```powershell
cd G:\TechStudyNotes\SystemDesignProjects\dropbox-file-sync-demo
mvn spring-boot:run
```

Open <http://127.0.0.1:8080>, then click **Run scenario**. The page creates a file from Device A, updates it, and commits a stale offline edit from Device B. Both contents are preserved.

Run verification:

```powershell
mvn test
powershell -ExecutionPolicy Bypass -File scripts\smoke-test.ps1
```

The first command runs the domain tests. The second builds and starts the actual HTTP server and verifies an end-to-end upload, update, conflict, download, and change log.

## Demo script for an interview

1. Click **Run scenario** and point to the three ordered events: `CREATE → UPDATE → CONFLICT`.
2. Explain that Device B submitted `baseVersion=1` after Device A had already committed version 2.
3. Show the two files. No write was silently lost; Device B became a conflict copy.
4. Download either file to show that metadata references real immutable blob bytes.
5. Rename a file. The event changes metadata but does not upload content again.
6. Delete a file. The row becomes a tombstone, allowing an offline client to learn about the deletion.
7. Upload identical content twice and show **Dedupe saved** increasing.
8. Restart the server and show that metadata and chunks survive under `data/`.

## Architecture represented

```text
Browser / simulated devices
        |
        | JSON metadata APIs
        v
Threaded HTTP API  ---> SyncStore metadata shard ---> metadata.json
        |
        | raw chunk upload/download
        v
Content-addressed blob plane ----------------------> data/blobs/<sha256>
```

`SyncStore` uses one lock to model linearizable metadata operations for a single user shard. A production service would partition accounts by `userId` and execute the same transaction rules in a replicated database.

### Commit protocol

1. Client chunks the file and calculates SHA-256 hashes.
2. `POST /api/uploads/plan` reports only missing chunks.
3. Client uploads missing chunks directly with `PUT /api/chunks/{hash}`.
4. `POST /api/commits` verifies every chunk and the complete file hash.
5. The server atomically stores the immutable version, latest pointer, idempotency result, and ordered event.

Metadata is never published before its referenced bytes pass integrity checks.

## API summary

| Operation      | Endpoint                                 | Important property                 |
| -------------- | ---------------------------------------- | ---------------------------------- |
| Plan upload    | `POST /api/uploads/plan`                 | Deduplicates by chunk hash         |
| Upload chunk   | `PUT /api/chunks/{sha256}`               | Rejects hash mismatch              |
| Commit version | `POST /api/commits`                      | Base-version check and idempotency |
| List state     | `GET /api/files`                         | Current metadata and tombstones    |
| Read log       | `GET /api/changes?cursor=N`              | Ordered, replayable changes        |
| Download       | `GET /api/files/{id}/download?version=N` | Integrity-verified assembly        |
| Rename         | `POST /api/files/{id}/move`              | Stable file ID; metadata-only      |
| Delete         | `DELETE /api/files/{id}?baseVersion=N`   | Durable tombstone                  |

Mutation APIs require an `Idempotency-Key`. A retried commit returns the original result without producing another file version or event.

## What is real vs simulated

| Demo implementation                   | Production Dropbox-style system                                         |
| ------------------------------------- | ----------------------------------------------------------------------- |
| One process and per-store lock        | Metadata service sharded by user ID                                     |
| Atomic replacement of `metadata.json` | Replicated transactional database plus outbox/log                       |
| Local content-addressed directory     | Multi-region object store with erasure coding/replication               |
| Browser polls every five seconds      | WebSocket/long-poll invalidation plus cursor polling                    |
| Client uses fixed 4 MiB chunks        | Adaptive/content-defined chunks and resumable multipart transfer        |
| No authentication                     | OAuth/device tokens, ACL checks, scoped signed URLs                     |
| Tombstones retained forever           | Retention window plus snapshot resync for expired cursors               |
| Manual demo devices                   | Native filesystem watcher, durable device journal, atomic local replace |

This is an architecturally faithful vertical slice, not a claim that a single-process demo has production scale or durability.

## Design decisions and trade-offs

- **Stable `fileId`:** rename changes `name`/`parentId`, so file bytes are not uploaded again.
- **Immutable versions:** supports recovery, integrity checks, and deterministic conflict handling at the cost of storage.
- **Conflict copies:** preserves data rather than silently selecting a winner. Users may need to merge manually.
- **At-least-once events:** consumers can retry safely because events have monotonically increasing server sequence numbers.
- **Server ordering:** client timestamps are never used for correctness, avoiding clock-skew bugs.
- **Chunk hashing:** enables resume and dedupe but consumes client CPU; a production client throttles work on battery.
- **Strong metadata, immutable blobs:** namespace state is serialized while large file bytes bypass application servers.

## Failure cases you can discuss

- A commit that times out is safely retried with the same idempotency key.
- A partial upload never becomes visible because commit rejects missing chunks.
- Wrong bytes under a claimed hash are rejected during upload.
- A stale edit becomes a conflict copy; stale rename/delete returns HTTP 409.
- Push notifications may be lost because `ListChanges(cursor)` is authoritative.
- Uncommitted chunks would be removed later by TTL/reachability garbage collection.
- If a cursor expires in production, the client installs a revisioned snapshot, then resumes its change cursor.
- Downloads should write a temporary local file, verify its hash, and atomically replace the destination.

## Interview pitches

### 30 seconds

“I separate strongly consistent metadata from immutable blob storage. Clients hash and upload only missing chunks, then atomically commit a version using a base version and idempotency key. Each commit updates the latest pointer and ordered per-user change log. Offline devices replay from a cursor. A stale update creates a conflict copy, so acknowledged data is never silently overwritten.”

### 2 minutes

Add the upload protocol, stable IDs for rename, tombstones for offline deletion, server sequence numbers, direct object-store URLs, CDN downloads, and sharding metadata by user. Close by distinguishing authoritative cursor reads from best-effort push invalidations.

### 5 minutes

Walk through the browser scenario, then cover database transaction boundaries, missing-chunk verification, retry idempotency, cursor expiration/snapshot recovery, chunk garbage collection, sync-storm backpressure, multi-region blob durability, and the trade-off between fixed and content-defined chunking.

## Project map

- `SyncService.java` — metadata and blob invariants; the main interview code.
- `SyncController.java` — HTTP API used by the browser and clients.
- `static/` — two-device dashboard and one-click conflict scenario.
- `SyncServiceTest.java` — correctness and failure-path tests.
- `scripts/smoke-test.ps1` — real HTTP end-to-end verification.
