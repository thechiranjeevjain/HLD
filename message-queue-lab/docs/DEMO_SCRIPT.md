# Message Queue Lab Demo Script

## Run

```powershell
cd G:\TechStudyNotes\SystemDesignProjects\message-queue-lab
mvn clean package
java -jar target/message-queue-lab-0.1.0-SNAPSHOT.jar
```

## Explain While It Runs

1. The producer appends three messages and assigns offsets.
2. Normal messages fail once to demonstrate retry.
3. On the second attempt, normal messages acknowledge successfully.
4. The poison message keeps failing.
5. After max attempts, the poison message moves to the DLQ.
6. Compaction removes acknowledged records from the front of the log.

## Interview Close

Say: queues decouple time, so the hard problems are retries, duplicates, ordering, backpressure, and poison-message isolation.
