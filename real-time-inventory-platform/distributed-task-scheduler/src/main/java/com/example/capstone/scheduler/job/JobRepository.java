package com.example.capstone.scheduler.job;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JobRepository extends JpaRepository<JobRecord, UUID> {

    Optional<JobRecord> findByIdempotencyKey(String idempotencyKey);

    List<JobRecord> findTop25ByStatusInAndRunAtLessThanEqualOrderByRunAtAsc(Collection<JobStatus> statuses, Instant runAt);
}
