package com.example.capstone.scheduler.job;

import com.example.capstone.scheduler.error.NotFoundException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class JobService {

    private final JobRepository jobRepository;

    public JobService(JobRepository jobRepository) {
        this.jobRepository = jobRepository;
    }

    public JobResponse create(CreateJobRequest request) {
        return jobRepository.findByIdempotencyKey(request.idempotencyKey().trim())
                .map(JobResponse::from)
                .orElseGet(() -> JobResponse.from(jobRepository.save(new JobRecord(
                        request.name().trim(),
                        request.payload().trim(),
                        request.runAt() == null ? Instant.now() : request.runAt(),
                        request.idempotencyKey().trim(),
                        request.maxAttempts() == null ? 3 : request.maxAttempts()
                ))));
    }

    @Transactional(readOnly = true)
    public List<JobResponse> list() {
        return jobRepository.findAll().stream().map(JobResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public JobResponse get(UUID id) {
        return jobRepository.findById(id)
                .map(JobResponse::from)
                .orElseThrow(() -> new NotFoundException("Job not found: " + id));
    }

    public JobResponse runNow(UUID id) {
        JobRecord job = jobRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Job not found: " + id));
        job.runNow(Instant.now());
        return JobResponse.from(job);
    }
}
