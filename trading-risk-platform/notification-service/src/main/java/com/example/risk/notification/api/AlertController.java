package com.example.risk.notification.api;

import com.example.risk.common.OrderEvent;
import com.example.risk.notification.domain.AlertRecord;
import com.example.risk.notification.service.NotificationService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/alerts")
public class AlertController {
    private final NotificationService notificationService;

    public AlertController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public AlertRecord publish(@RequestBody OrderEvent event) {
        return notificationService.publish(event);
    }

    @GetMapping
    public List<AlertRecord> recent() {
        return notificationService.recent();
    }
}
