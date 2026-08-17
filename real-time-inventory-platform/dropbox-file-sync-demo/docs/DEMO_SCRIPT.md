# Dropbox File Sync Demo Script

## Verify

```powershell
cd G:\TechStudyNotes\SystemDesignProjects\dropbox-file-sync-demo
mvn test
powershell -ExecutionPolicy Bypass -File scripts\smoke-test.ps1
```

Expected result: unit tests pass, then the smoke script prints `PASS: real HTTP plan, chunk upload, metadata commit, download, and cursor log`.

## Run

```powershell
cd G:\TechStudyNotes\SystemDesignProjects\dropbox-file-sync-demo
mvn spring-boot:run
```

Open `http://127.0.0.1:8080`.

## Walkthrough

1. Click `Run scenario`.
2. Point out the ordered events: create, update, and conflict.
3. Explain that Device B submitted an old `baseVersion`.
4. Show that the stale edit became a conflict copy instead of overwriting Device A.
5. Download either file and explain immutable blob reconstruction.
6. Rename a file and show that bytes are not uploaded again.
7. Delete a file and explain tombstones for offline devices.
8. Restart the server and show that metadata and chunks persist under `data/`.

## Interview Close

Say: the design protects acknowledged data with base-version checks, idempotency, immutable versions, and ordered cursor replay. The demo is intentionally single-process, but the same transaction boundary maps to a sharded metadata service plus object storage.
