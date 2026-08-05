package com.example.risk.history.messaging;

import com.example.risk.common.OrderEvent;
import com.example.risk.history.service.ExposureService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class OrderEventConsumer {
    private static final Logger log = LoggerFactory.getLogger(OrderEventConsumer.class);

    private final ExposureService exposureService;

    public OrderEventConsumer(ExposureService exposureService) {
        this.exposureService = exposureService;
    }

    @KafkaListener(topics = "${app.kafka.topics.order-events}", groupId = "${spring.kafka.consumer.group-id}")
    void onOrderEvent(OrderEvent event) {
        log.info("history-service received order event orderId={} status={}", event.orderId(), event.status());
        exposureService.record(event);
    }
}

