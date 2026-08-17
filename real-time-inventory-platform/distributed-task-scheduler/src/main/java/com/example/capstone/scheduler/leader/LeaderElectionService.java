package com.example.capstone.scheduler.leader;

import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LeaderElectionService {

    private static final String LOCK_NAME = "scheduler-leader";

    private final SchedulerLockRepository lockRepository;
    private final String instanceId = UUID.randomUUID().toString();

    public LeaderElectionService(SchedulerLockRepository lockRepository) {
        this.lockRepository = lockRepository;
    }

    @Transactional
    public boolean tryAcquireLeadership(Instant now) {
        SchedulerLock lock = lockRepository.findByLockName(LOCK_NAME)
                .orElseGet(() -> lockRepository.save(new SchedulerLock(LOCK_NAME, instanceId, now.plusSeconds(15))));
        if (instanceId.equals(lock.getOwnerId()) || lock.getLockedUntil().isBefore(now)) {
            lock.renew(instanceId, now.plusSeconds(15));
            return true;
        }
        return false;
    }

    public String instanceId() {
        return instanceId;
    }
}
