package com.example.capstone.scheduler.job;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JobExecutionRepository extends JpaRepository<JobExecutionRecord, UUID> {

    boolean existsByIdempotencyKey(String idempotencyKey);
}
