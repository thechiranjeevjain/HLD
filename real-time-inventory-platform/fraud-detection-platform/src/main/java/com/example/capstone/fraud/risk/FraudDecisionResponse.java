package com.example.capstone.fraud.risk;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record FraudDecisionResponse(
        UUID id,
        String transactionId,
        String userId,
        int riskScore,
        RiskLevel riskLevel,
        List<String> reasons,
        Instant createdAt
) {

    public static FraudDecisionResponse from(FraudDecision decision) {
        return new FraudDecisionResponse(
                decision.getId(),
                decision.getTransactionId(),
                decision.getUserId(),
                decision.getRiskScore(),
                decision.getRiskLevel(),
                decision.getReasons(),
                decision.getCreatedAt()
        );
    }
}
