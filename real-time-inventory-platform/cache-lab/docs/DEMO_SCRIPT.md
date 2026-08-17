# Cache Lab Demo Script

## Run

```powershell
cd G:\TechStudyNotes\SystemDesignProjects\cache-lab
mvn clean package
java -jar target/cache-lab-0.1.0-SNAPSHOT.jar
```

## Explain While It Runs

1. `PUT A=1` stores an entry and moves it to the most-recently-used position.
2. `GET A` is a hit.
3. After the sleep, `GET A after TTL` is a miss because TTL expired.
4. Inserting `B`, `C`, and `D` exceeds capacity and evicts the least-recently-used key.
5. The final stats show hits, misses, evictions, expirations, and size.

## Interview Close

Say: eviction protects memory, TTL bounds staleness, and the backing source of truth must still own correctness.
