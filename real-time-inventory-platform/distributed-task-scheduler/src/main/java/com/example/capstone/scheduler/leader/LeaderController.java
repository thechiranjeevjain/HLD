package com.example.capstone.scheduler.leader;

import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class LeaderController {

    private final LeaderElectionService leaderElectionService;

    public LeaderController(LeaderElectionService leaderElectionService) {
        this.leaderElectionService = leaderElectionService;
    }

    @GetMapping("/api/leader")
    public Map<String, String> leader() {
        return Map.of("instanceId", leaderElectionService.instanceId());
    }
}
