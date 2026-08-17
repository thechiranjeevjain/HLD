package com.example.capstone.notification.notification;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<NotificationRecord, UUID> {

    List<NotificationRecord> findTop50ByStatusInAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(
            Collection<NotificationStatus> statuses,
            Instant nextAttemptAt
    );
}
