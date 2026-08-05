package com.example.risk.common;

import java.math.BigDecimal;

public record ExposureSummary(
        String clientId,
        String symbol,
        long netQuantity,
        BigDecimal grossNotional,
        BigDecimal dailyExposure
) {
    public static ExposureSummary zero(String clientId, String symbol) {
        return new ExposureSummary(clientId, symbol, 0, BigDecimal.ZERO, BigDecimal.ZERO);
    }
}

