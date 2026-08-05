package com.example.risk.common;

import java.util.UUID;

public record OrderResponse(
        UUID orderId,
        OrderStatus status,
        String reason
) {
}

