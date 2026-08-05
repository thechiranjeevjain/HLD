package com.example.capstone.fraud.risk;

public enum RiskLevel {
    LOW,
    MEDIUM,
    HIGH;

    static RiskLevel fromScore(int score) {
        if (score >= 70) {
            return HIGH;
        }
        if (score >= 40) {
            return MEDIUM;
        }
        return LOW;
    }
}
