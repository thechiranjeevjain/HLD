package com.example.capstone.notification.notification;

import com.example.capstone.notification.delivery.DeliveryException;
import com.example.capstone.notification.delivery.DeliveryRouter;
import com.example.capstone.notification.dlq.DeadLetterRecord;
import com.example.capstone.notification.dlq.DeadLetterRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class NotificationService {

    private static final List<NotificationStatus> DUE_STATUSES = List.of(
            NotificationStatus.PENDING,
            NotificationStatus.RETRY_SCHEDULED
    );

    private final NotificationRepository notificationRepository;
    private final DeadLetterRepository deadLetterRepository;
    private final DeliveryRouter deliveryRouter;

    public NotificationService(
            NotificationRepository notificationRepository,
            DeadLetterRepository deadLetterRepository,
            DeliveryRouter deliveryRouter
    ) {
        this.notificationRepository = notificationRepository;
        this.deadLetterRepository = deadLetterRepository;
        this.deliveryRouter = deliveryRouter;
    }

    public NotificationResponse create(CreateNotificationRequest request) {
        NotificationRecord notification = new NotificationRecord(
                request.channel(),
                request.recipient().trim(),
                request.subject() == null ? null : request.subject().trim(),
                request.body().trim(),
                request.maxAttempts() == null ? 3 : request.maxAttempts(),
                Instant.now()
        );
        return NotificationResponse.from(notificationRepository.save(notification));
    }

    @Transactional(readOnly = true)
    public NotificationResponse get(UUID id) {
        return notificationRepository.findById(id)
                .map(NotificationResponse::from)
                .orElseThrow(() -> new NotificationNotFoundException(id));
    }

    public NotificationResponse manualRetry(UUID id) {
        NotificationRecord notification = notificationRepository.findById(id)
                .orElseThrow(() -> new NotificationNotFoundException(id));
        notification.requeueManually(Instant.now());
        return NotificationResponse.from(notification);
    }

    @Scheduled(fixedDelayString = "${app.worker.fixed-delay-ms:5000}")
    public void processDueNotifications() {
        Instant now = Instant.now();
        notificationRepository.findTop50ByStatusInAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(DUE_STATUSES, now)
                .forEach(notification -> {
                    try {
                        attemptDelivery(notification, Instant.now());
                    } catch (RuntimeException ignored) {
                        // The worker keeps moving even when one record is malformed.
                    }
                });
    }

    public void attemptDelivery(NotificationRecord notification, Instant now) {
        try {
            deliveryRouter.deliver(notification);
            notification.markSent(now);
        } catch (DeliveryException exception) {
            notification.markFailure(exception.getMessage(), now);
            if (notification.getStatus() == NotificationStatus.DEAD_LETTER
                    && notification.getId() != null
                    && !deadLetterRepository.existsByNotificationId(notification.getId())) {
                deadLetterRepository.save(new DeadLetterRecord(
                        notification.getId(),
                        notification.getChannel(),
                        notification.getRecipient(),
                        exception.getMessage()
                ));
            }
        }
    }
}
