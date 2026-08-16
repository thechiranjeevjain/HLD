# Dropbox File Sync Demo Interview Guide

## Two-Minute Pitch

This project models the core correctness rules behind Dropbox-style sync. Clients upload only missing content chunks, then commit a file version with a base version and idempotency key. Metadata changes are serialized, versions are immutable, and stale edits become conflict copies instead of silently overwriting acknowledged data.

## What To Emphasize

- The system separates strongly consistent metadata from immutable blob storage.
- Content-addressed chunks make retry, resume, and dedupe visible.
- `baseVersion` protects the check-then-commit namespace invariant.
- `Idempotency-Key` makes retried mutations safe.
- Ordered cursor events let offline clients catch up after missed push notifications.
- Tombstones preserve delete knowledge for devices that were offline.

## Request Flow

1. A client hashes file chunks and asks `POST /api/uploads/plan` which chunks are missing.
2. Missing chunks are uploaded through `PUT /api/chunks/{sha256}`.
3. `POST /api/commits` verifies chunk presence, file hash, base version, and idempotency.
4. The server atomically publishes an immutable version and latest pointer.
5. The server appends an ordered change event for cursor replay.
6. Offline clients call `GET /api/changes?cursor=N` and reconcile local state.

## Tradeoffs

| Decision             | Benefit                             | Cost                                         |
| -------------------- | ----------------------------------- | -------------------------------------------- |
| Stable `fileId`      | Rename does not re-upload bytes     | Requires separate namespace metadata         |
| Immutable versions   | Easy recovery and conflict handling | Storage grows until garbage collection       |
| Conflict copies      | No acknowledged write is lost       | Users may need to merge manually             |
| Fixed-size chunks    | Simple deterministic demo           | Less efficient than content-defined chunking |
| Single metadata lock | Clear linearizable shard model      | Not horizontally scalable as-is              |
| Local blob directory | Runnable without cloud services     | No production durability guarantee           |

## FAQ

Q: Why is this not just a file upload service?
A: A plain upload service stores bytes. Sync also needs version history, conflict detection, rename/delete semantics, idempotency, and replayable changes for offline devices.

Q: What happens when two devices edit the same file offline?
A: The stale commit fails the base-version check and is preserved as a conflict copy, so neither edit silently disappears.

Q: Why separate metadata and blobs?
A: Metadata needs transaction ordering. Large immutable bytes can move through a cheaper object-storage path once integrity is verified.

Q: What would you add next?
A: authentication, account sharding, signed object-store URLs, cursor expiration snapshots, chunk garbage collection, push invalidations, and client-side filesystem watchers.
