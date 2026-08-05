package com.example.capstone.notification.dlq;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeadLetterRepository extends JpaRepository<DeadLetterRecord, UUID> {

    boolean existsByNotificationId(UUID notificationId);
}
