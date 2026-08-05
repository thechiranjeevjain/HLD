package com.example.risk.order.messaging;

import com.example.risk.common.OrderEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class OrderEventPublisher {
    private final KafkaTemplate<String, OrderEvent> kafkaTemplate;
    private final String topicName;

    public OrderEventPublisher(
            KafkaTemplate<String, OrderEvent> kafkaTemplate,
            @Value("${app.kafka.topics.order-events}") String topicName
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.topicName = topicName;
    }

    public void publish(OrderEvent event) {
        kafkaTemplate.send(topicName, event.clientId(), event);
    }
}

