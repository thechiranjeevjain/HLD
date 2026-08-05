package com.example.capstone.ecommerce.inventory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.capstone.ecommerce.error.DomainException;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class InventoryItemTest {

    @Test
    void reserveMovesStockFromAvailableToReserved() {
        InventoryItem item = new InventoryItem("SKU-1001", "Keyboard", new BigDecimal("129.99"), "USD", 10);

        item.reserve(3);

        assertThat(item.getAvailableQuantity()).isEqualTo(7);
        assertThat(item.getReservedQuantity()).isEqualTo(3);
    }

    @Test
    void releaseReservedReturnsStockToAvailable() {
        InventoryItem item = new InventoryItem("SKU-1001", "Keyboard", new BigDecimal("129.99"), "USD", 10);

        item.reserve(3);
        item.releaseReserved(2);

        assertThat(item.getAvailableQuantity()).isEqualTo(9);
        assertThat(item.getReservedQuantity()).isEqualTo(1);
    }

    @Test
    void reserveRejectsOversell() {
        InventoryItem item = new InventoryItem("SKU-1001", "Keyboard", new BigDecimal("129.99"), "USD", 1);

        assertThatThrownBy(() -> item.reserve(2))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("Insufficient inventory");
    }
}
