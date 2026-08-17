package com.example.capstone.ecommerce.inventory;

import java.math.BigDecimal;
import java.util.UUID;

public record InventoryResponse(
        UUID id,
        String sku,
        String name,
        BigDecimal price,
        String currency,
        int availableQuantity,
        int reservedQuantity
) {

    public static InventoryResponse from(InventoryItem item) {
        return new InventoryResponse(
                item.getId(),
                item.getSku(),
                item.getName(),
                item.getPrice(),
                item.getCurrency(),
                item.getAvailableQuantity(),
                item.getReservedQuantity()
        );
    }
}
