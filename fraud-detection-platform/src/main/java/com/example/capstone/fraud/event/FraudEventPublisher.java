package com.example.capstone.fraud.event;

import com.example.capstone.fraud.transaction.TransactionEventRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class FraudEventPublisher {

    private static final Logger LOGGER = LoggerFactory.getLogger(FraudEventPublisher.class);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final String topic;

    public FraudEventPublisher(
            KafkaTemplate<String, String> kafkaTemplate,
            ObjectMapper objectMapper,
            @Value("${app.kafka.transaction-events-topic}") String topic
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.topic = topic;
    }

    public void publish(TransactionEventRequest request) {
        try {
            kafkaTemplate.send(topic, request.transactionId(), objectMapper.writeValueAsString(request));
        } catch (JsonProcessingException | RuntimeException exception) {
            LOGGER.warn("Unable to publish transaction event {}", request.transactionId(), exception);
        }
    }
}
