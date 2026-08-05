package com.example.capstone.fraud.risk;

import com.example.capstone.fraud.error.NotFoundException;
import com.example.capstone.fraud.transaction.TransactionEvent;
import com.example.capstone.fraud.transaction.TransactionEventRequest;
import com.example.capstone.fraud.velocity.VelocityService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class FraudScoringService {

    private final FraudDecisionRepository decisionRepository;
    private final FraudRuleEngine ruleEngine;
    private final VelocityService velocityService;

    public FraudScoringService(
            FraudDecisionRepository decisionRepository,
            FraudRuleEngine ruleEngine,
            VelocityService velocityService
    ) {
        this.decisionRepository = decisionRepository;
        this.ruleEngine = ruleEngine;
        this.velocityService = velocityService;
    }

    public FraudDecisionResponse score(TransactionEventRequest request) {
        return decisionRepository.findByTransactionId(request.transactionId().trim())
                .map(FraudDecisionResponse::from)
                .orElseGet(() -> {
                    TransactionEvent event = request.toEvent();
                    long velocityCount = velocityService.recordAndCount(event);
                    RuleEvaluation evaluation = ruleEngine.evaluate(event, velocityCount);
                    FraudDecision decision = new FraudDecision(
                            event.transactionId(),
                            event.userId(),
                            evaluation.riskScore(),
                            evaluation.riskLevel(),
                            evaluation.reasons()
                    );
                    return FraudDecisionResponse.from(decisionRepository.save(decision));
                });
    }

    @Transactional(readOnly = true)
    public FraudDecisionResponse get(String transactionId) {
        return decisionRepository.findByTransactionId(transactionId)
                .map(FraudDecisionResponse::from)
                .orElseThrow(() -> new NotFoundException("Fraud decision not found: " + transactionId));
    }
}
