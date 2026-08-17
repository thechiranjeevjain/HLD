package com.example.risk.risk.service;

import com.example.risk.risk.domain.RiskLimit;

import java.math.BigDecimal;

public record RiskLimitSnapshot(
        String clientId,
        String symbol,
        long maxOrderQuantity,
        long maxPositionQuantity,
        BigDecimal maxDailyExposure
) {
    public static RiskLimitSnapshot from(RiskLimit limit) {
        return new RiskLimitSnapshot(
                limit.getClientId(),
                limit.getSymbol(),
                limit.getMaxOrderQuantity(),
                limit.getMaxPositionQuantity(),
                limit.getMaxDailyExposure()
        );
    }
}

