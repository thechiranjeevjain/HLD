# Message Queue Lab

A small Java 17 lab for understanding producers, offsets, retries, acknowledgments, and dead-letter queues. The queue is single-process and in-memory so the delivery semantics are easy to inspect.

## What It Shows

- Monotonic offsets.
- At-least-once delivery.
- Explicit acknowledgments.
- Retry delay after consumer failure.
- Dead-letter movement after max attempts.
- Log compaction for acknowledged records.

## Run

```powershell
cd G:\TechStudyNotes\SystemDesignProjects\message-queue-lab
mvn clean package
java -jar target/message-queue-lab-0.1.0-SNAPSHOT.jar
```

## Expected Demo Behavior

The demo produces `order-1`, `poison`, and `order-2`. Normal messages fail once and then acknowledge. The poison message keeps failing and moves to the dead-letter log.

## Learning Docs

- [Interview Guide](docs/INTERVIEW_GUIDE.md)
- [Diagrams](docs/DIAGRAMS.md)
- [Demo Script](docs/DEMO_SCRIPT.md)
