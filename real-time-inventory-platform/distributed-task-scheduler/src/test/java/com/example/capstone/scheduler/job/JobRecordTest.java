package com.example.capstone.scheduler.job;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class JobRecordTest {

    @Test
    void failureSchedulesRetryBeforeMaxAttempts() {
        Instant now = Instant.parse("2026-08-05T00:00:00Z");
        JobRecord job = new JobRecord("report", "fail once", now, "report-1", 3);

        job.start("worker-1", now);
        job.markFailure("temporary failure", now);

        assertThat(job.getStatus()).isEqualTo(JobStatus.RETRY_SCHEDULED);
        assertThat(job.getAttempts()).isEqualTo(1);
        assertThat(job.getRunAt()).isEqualTo(now.plusSeconds(15));
    }

    @Test
    void failureMarksJobFailedAtMaxAttempts() {
        Instant now = Instant.parse("2026-08-05T00:00:00Z");
        JobRecord job = new JobRecord("report", "fail", now, "report-2", 1);

        job.start("worker-1", now);
        job.markFailure("permanent failure", now);

        assertThat(job.getStatus()).isEqualTo(JobStatus.FAILED);
        assertThat(job.getCompletedAt()).isEqualTo(now);
    }
}
