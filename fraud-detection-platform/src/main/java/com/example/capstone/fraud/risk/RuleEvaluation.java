package com.example.capstone.fraud.risk;

import java.util.List;

public record RuleEvaluation(
        int riskScore,
        RiskLevel riskLevel,
        List<String> reasons
) {
}
