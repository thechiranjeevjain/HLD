package com.example.risk.common;

public record RiskCheckResponse(
        RiskDecision decision,
        String reason
) {
    public static RiskCheckResponse accept(String reason) {
        return new RiskCheckResponse(RiskDecision.ACCEPT, reason);
    }

    public static RiskCheckResponse reject(String reason) {
        return new RiskCheckResponse(RiskDecision.REJECT, reason);
    }
}

