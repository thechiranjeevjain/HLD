package com.example.capstone.fraud.risk;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.capstone.fraud.transaction.TransactionEvent;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class FraudRuleEngineTest {

    private final FraudRuleEngine ruleEngine = new FraudRuleEngine();

    @Test
    void highRiskSignalsProduceHighRiskScore() {
        TransactionEvent event = new TransactionEvent(
                "txn-1",
                "user-1",
                new BigDecimal("6200.00"),
                "USD",
                "CRYPTO",
                "SG",
                "US",
                false,
                Instant.now()
        );

        RuleEvaluation evaluation = ruleEngine.evaluate(event, 6);

        assertThat(evaluation.riskLevel()).isEqualTo(RiskLevel.HIGH);
        assertThat(evaluation.riskScore()).isEqualTo(100);
        assertThat(evaluation.reasons()).contains("Very large transaction amount", "High transaction velocity for user");
    }

    @Test
    void normalTransactionProducesLowRiskScore() {
        TransactionEvent event = new TransactionEvent(
                "txn-2",
                "user-1",
                new BigDecimal("42.00"),
                "USD",
                "GROCERY",
                "US",
                "US",
                true,
                Instant.now()
        );

        RuleEvaluation evaluation = ruleEngine.evaluate(event, 1);

        assertThat(evaluation.riskLevel()).isEqualTo(RiskLevel.LOW);
        assertThat(evaluation.riskScore()).isZero();
    }
}
