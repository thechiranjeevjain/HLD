package io.exchangelite.engine.core;

public record RiskDecision(boolean accepted, String reason) {
    public static RiskDecision allow() {
        return new RiskDecision(true, "accepted");
    }

    public static RiskDecision rejected(String reason) {
        return new RiskDecision(false, reason);
    }
}
