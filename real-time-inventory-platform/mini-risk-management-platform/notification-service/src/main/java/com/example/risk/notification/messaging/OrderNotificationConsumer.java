package com.example.risk.notification.messaging;

import com.example.risk.common.OrderEvent;
import com.example.risk.notification.service.NotificationFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class OrderNotificationConsumer {
    private static final Logger log = LoggerFactory.getLogger(OrderNotificationConsumer.class);

    private final NotificationFormatter formatter;

    public OrderNotificationConsumer(NotificationFormatter formatter) {
        this.formatter = formatter;
    }

    @KafkaListener(topics = "${app.kafka.topics.order-events}", groupId = "${spring.kafka.consumer.group-id}")
    void onOrderEvent(OrderEvent event) {
        log.info("simulated-email-notification {}", formatter.format(event));
    }
}

