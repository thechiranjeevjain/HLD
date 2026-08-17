package com.example.capstone.notification.notification;

import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @PostMapping
    public NotificationResponse create(@Valid @RequestBody CreateNotificationRequest request) {
        return notificationService.create(request);
    }

    @GetMapping("/{id}")
    public NotificationResponse get(@PathVariable UUID id) {
        return notificationService.get(id);
    }

    @PostMapping("/{id}/retry")
    public NotificationResponse retry(@PathVariable UUID id) {
        return notificationService.manualRetry(id);
    }
}
