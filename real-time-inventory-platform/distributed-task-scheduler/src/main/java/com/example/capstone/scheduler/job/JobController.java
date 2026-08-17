package com.example.capstone.scheduler.job;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/jobs")
public class JobController {

    private final JobService jobService;

    public JobController(JobService jobService) {
        this.jobService = jobService;
    }

    @PostMapping
    public JobResponse create(@Valid @RequestBody CreateJobRequest request) {
        return jobService.create(request);
    }

    @GetMapping
    public List<JobResponse> list() {
        return jobService.list();
    }

    @GetMapping("/{id}")
    public JobResponse get(@PathVariable UUID id) {
        return jobService.get(id);
    }

    @PostMapping("/{id}/run-now")
    public JobResponse runNow(@PathVariable UUID id) {
        return jobService.runNow(id);
    }
}
