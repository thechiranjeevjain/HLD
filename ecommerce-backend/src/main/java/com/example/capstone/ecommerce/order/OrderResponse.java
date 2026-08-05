package com.example.capstone.ecommerce.order;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OrderResponse(
        UUID id,
        String customerId,
        OrderStatus status,
        BigDecimal totalAmount,
        String currency,
        List<OrderLineResponse> items,
        Instant createdAt
) {

    public static OrderResponse from(CustomerOrder order) {
        return new OrderResponse(
                order.getId(),
                order.getCustomerId(),
                order.getStatus(),
                order.getTotalAmount(),
                order.getCurrency(),
                order.getLines().stream().map(OrderLineResponse::from).toList(),
                order.getCreatedAt()
        );
    }
}
