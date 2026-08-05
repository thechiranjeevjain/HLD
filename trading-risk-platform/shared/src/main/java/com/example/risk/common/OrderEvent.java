package com.example.risk.common;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record OrderEvent(
        UUID orderId,
        String clientId,
        String symbol,
        OrderSide side,
        long quantity,
        BigDecimal price,
        BigDecimal notional,
        OrderStatus status,
        String reason,
        Instant occurredAt
) {
}

