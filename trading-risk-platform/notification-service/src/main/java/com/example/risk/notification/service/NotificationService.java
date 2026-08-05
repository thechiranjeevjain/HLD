package com.example.risk.notification.service;

import com.example.risk.common.OrderEvent;
import com.example.risk.notification.domain.AlertRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

@Service
public class NotificationService {
    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);
    private static final int MAX_RECENT_ALERTS = 100;

    private final Deque<AlertRecord> recentAlerts = new ArrayDeque<>();

    public synchronized AlertRecord publish(OrderEvent event) {
        AlertRecord alert = new AlertRecord(
                event.orderId(),
                event.clientId(),
                event.symbol(),
                event.status(),
                event.notional(),
                event.status() + " " + event.symbol() + " order for " + event.clientId() + ": " + event.reason(),
                Instant.now());

        recentAlerts.addFirst(alert);
        while (recentAlerts.size() > MAX_RECENT_ALERTS) {
            recentAlerts.removeLast();
        }

        log.warn("Risk alert published: {}", alert);
        return alert;
    }

    public synchronized List<AlertRecord> recent() {
        return new ArrayList<>(recentAlerts);
    }
}
