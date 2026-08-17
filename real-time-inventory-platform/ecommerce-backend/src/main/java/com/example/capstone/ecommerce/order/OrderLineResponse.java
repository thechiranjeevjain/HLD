package com.example.capstone.ecommerce.order;

import java.math.BigDecimal;

public record OrderLineResponse(
        String sku,
        String name,
        int quantity,
        BigDecimal unitPrice,
        BigDecimal lineTotal
) {

    public static OrderLineResponse from(OrderLine line) {
        return new OrderLineResponse(
                line.getSku(),
                line.getName(),
                line.getQuantity(),
                line.getUnitPrice(),
                line.getLineTotal()
        );
    }
}
