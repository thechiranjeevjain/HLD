package com.example.capstone.fraud.event;

import com.example.capstone.fraud.risk.FraudScoringService;
import com.example.capstone.fraud.transaction.TransactionEventRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class FraudEventListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(FraudEventListener.class);

    private final ObjectMapper objectMapper;
    private final FraudScoringService fraudScoringService;

    public FraudEventListener(ObjectMapper objectMapper, FraudScoringService fraudScoringService) {
        this.objectMapper = objectMapper;
        this.fraudScoringService = fraudScoringService;
    }

    @KafkaListener(topics = "${app.kafka.transaction-events-topic}", groupId = "${spring.kafka.consumer.group-id}")
    public void onTransaction(String payload) {
        try {
            fraudScoringService.score(objectMapper.readValue(payload, TransactionEventRequest.class));
        } catch (Exception exception) {
            LOGGER.warn("Unable to process transaction event", exception);
        }
    }
}
