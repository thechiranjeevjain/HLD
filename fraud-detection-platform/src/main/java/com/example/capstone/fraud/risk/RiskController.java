package com.example.capstone.fraud.risk;

import com.example.capstone.fraud.event.FraudEventPublisher;
import com.example.capstone.fraud.transaction.TransactionEventRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class RiskController {

    private final FraudScoringService fraudScoringService;
    private final FraudEventPublisher fraudEventPublisher;

    public RiskController(FraudScoringService fraudScoringService, FraudEventPublisher fraudEventPublisher) {
        this.fraudScoringService = fraudScoringService;
        this.fraudEventPublisher = fraudEventPublisher;
    }

    @PostMapping("/events/transactions")
    public FraudDecisionResponse ingest(@Valid @RequestBody TransactionEventRequest request) {
        FraudDecisionResponse response = fraudScoringService.score(request);
        fraudEventPublisher.publish(request);
        return response;
    }

    @GetMapping("/risks/{transactionId}")
    public FraudDecisionResponse get(@PathVariable String transactionId) {
        return fraudScoringService.get(transactionId);
    }
}
