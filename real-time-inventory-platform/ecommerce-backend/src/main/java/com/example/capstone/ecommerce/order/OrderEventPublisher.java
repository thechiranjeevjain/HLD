package com.example.capstone.ecommerce.order;

import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class OrderEventPublisher {

    private static final Logger LOGGER = LoggerFactory.getLogger(OrderEventPublisher.class);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final String topic;

    public OrderEventPublisher(KafkaTemplate<String, String> kafkaTemplate, @Value("${app.kafka.order-events-topic}") String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
    }

    public void publish(UUID orderId, String eventType, OrderStatus status) {
        String payload = """
                {"orderId":"%s","eventType":"%s","status":"%s"}
                """.formatted(orderId, eventType, status).trim();
        try {
            kafkaTemplate.send(topic, orderId.toString(), payload);
        } catch (RuntimeException exception) {
            LOGGER.warn("Unable to publish order event {}", eventType, exception);
        }
    }
}
