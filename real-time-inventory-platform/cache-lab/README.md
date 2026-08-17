# Cache Lab

A small Java 17 lab for understanding LRU eviction, TTL expiry, bounded memory, and cache observability. The implementation is intentionally single-process and explicit so cache correctness boundaries are visible in code.

## What It Shows

- LRU ordering with a doubly linked list.
- TTL expiry using monotonic time.
- Bounded capacity with eviction.
- Hit, miss, eviction, and expiration counters.
- A single synchronization boundary around cache mutation.

## Run

```powershell
cd G:\TechStudyNotes\SystemDesignProjects\cache-lab
mvn clean package
java -jar target/cache-lab-0.1.0-SNAPSHOT.jar
```

## Expected Demo Behavior

The demo puts key `A`, reads it, waits for TTL expiry, then inserts keys `B`, `C`, and `D` to force LRU eviction. It prints cache misses and stats at the end.

## Learning Docs

- [Interview Guide](docs/INTERVIEW_GUIDE.md)
- [Diagrams](docs/DIAGRAMS.md)
- [Demo Script](docs/DEMO_SCRIPT.md)
