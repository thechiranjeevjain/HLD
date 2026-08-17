package com.example.risk.notification.domain;

import com.example.risk.common.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record AlertRecord(
        UUID orderId,
        String clientId,
        String symbol,
        OrderStatus status,
        BigDecimal notional,
        String message,
        Instant publishedAt
) {
}
