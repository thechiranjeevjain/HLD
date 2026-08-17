package com.example.risk.notification.service;

import com.example.risk.common.OrderEvent;
import org.springframework.stereotype.Component;

@Component
public class NotificationFormatter {
    public String format(OrderEvent event) {
        return "email=ops@mini-risk.local orderId=%s client=%s symbol=%s side=%s quantity=%d status=%s reason=\"%s\""
                .formatted(
                        event.orderId(),
                        event.clientId(),
                        event.symbol(),
                        event.side(),
                        event.quantity(),
                        event.status(),
                        event.reason()
                );
    }
}

