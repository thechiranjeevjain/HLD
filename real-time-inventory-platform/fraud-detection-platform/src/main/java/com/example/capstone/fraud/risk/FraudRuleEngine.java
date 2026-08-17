package com.example.capstone.fraud.risk;

import com.example.capstone.fraud.transaction.TransactionEvent;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class FraudRuleEngine {

    private static final Set<String> HIGH_RISK_MERCHANTS = Set.of("CRYPTO", "GAMBLING", "GIFT_CARD");

    public RuleEvaluation evaluate(TransactionEvent event, long velocityCount) {
        int score = 0;
        List<String> reasons = new ArrayList<>();

        if (event.amount().compareTo(new BigDecimal("5000.00")) >= 0) {
            score += 60;
            reasons.add("Very large transaction amount");
        } else if (event.amount().compareTo(new BigDecimal("1000.00")) >= 0) {
            score += 35;
            reasons.add("Large transaction amount");
        }

        if (!event.country().equals(event.homeCountry())) {
            score += 25;
            reasons.add("Transaction country differs from home country");
        }

        if (HIGH_RISK_MERCHANTS.contains(event.merchantCategory())) {
            score += 20;
            reasons.add("High-risk merchant category");
        }

        if (!event.cardPresent() && event.amount().compareTo(new BigDecimal("500.00")) >= 0) {
            score += 10;
            reasons.add("Card-not-present high-value transaction");
        }

        if (velocityCount >= 5) {
            score += 30;
            reasons.add("High transaction velocity for user");
        }

        int cappedScore = Math.min(score, 100);
        if (reasons.isEmpty()) {
            reasons.add("No high-risk rules matched");
        }

        return new RuleEvaluation(cappedScore, RiskLevel.fromScore(cappedScore), reasons);
    }
}
