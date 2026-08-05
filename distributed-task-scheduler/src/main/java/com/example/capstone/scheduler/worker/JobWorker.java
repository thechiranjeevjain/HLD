package com.example.capstone.scheduler.worker;

import com.example.capstone.scheduler.job.JobExecutionRecord;
import com.example.capstone.scheduler.job.JobExecutionRepository;
import com.example.capstone.scheduler.job.JobRecord;
import com.example.capstone.scheduler.job.JobRepository;
import com.example.capstone.scheduler.job.JobStatus;
import com.example.capstone.scheduler.leader.LeaderElectionService;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class JobWorker {

    private static final List<JobStatus> DUE_STATUSES = List.of(JobStatus.QUEUED, JobStatus.RETRY_SCHEDULED);

    private final LeaderElectionService leaderElectionService;
    private final JobRepository jobRepository;
    private final JobExecutionRepository executionRepository;

    public JobWorker(
            LeaderElectionService leaderElectionService,
            JobRepository jobRepository,
            JobExecutionRepository executionRepository
    ) {
        this.leaderElectionService = leaderElectionService;
        this.jobRepository = jobRepository;
        this.executionRepository = executionRepository;
    }

    @Scheduled(fixedDelayString = "${app.worker.fixed-delay-ms:3000}")
    @Transactional
    public void poll() {
        Instant now = Instant.now();
        if (!leaderElectionService.tryAcquireLeadership(now)) {
            return;
        }

        jobRepository.findTop25ByStatusInAndRunAtLessThanEqualOrderByRunAtAsc(DUE_STATUSES, now)
                .forEach(job -> execute(job, now));
    }

    void execute(JobRecord job, Instant now) {
        job.start(leaderElectionService.instanceId(), now);
        if (executionRepository.existsByIdempotencyKey(job.getIdempotencyKey())) {
            job.markSucceeded(now);
            return;
        }

        try {
            if (job.getPayload().toLowerCase(Locale.ROOT).contains("fail")) {
                throw new IllegalStateException("Simulated job failure");
            }
            executionRepository.save(new JobExecutionRecord(job.getId(), job.getIdempotencyKey(), "OK"));
            job.markSucceeded(now);
        } catch (RuntimeException exception) {
            job.markFailure(exception.getMessage(), now);
        }
    }
}
