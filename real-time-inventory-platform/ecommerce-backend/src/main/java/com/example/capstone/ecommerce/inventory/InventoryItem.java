package com.example.capstone.ecommerce.inventory;

import com.example.capstone.ecommerce.error.DomainException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "inventory_items")
public class InventoryItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 64)
    private String sku;

    @Column(nullable = false, length = 160)
    private String name;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal price;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(nullable = false)
    private int availableQuantity;

    @Column(nullable = false)
    private int reservedQuantity;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    protected InventoryItem() {
    }

    public InventoryItem(String sku, String name, BigDecimal price, String currency, int availableQuantity) {
        this.sku = sku;
        this.name = name;
        this.price = price;
        this.currency = currency;
        this.availableQuantity = availableQuantity;
    }

    public void replace(String name, BigDecimal price, String currency, int stockQuantity) {
        this.name = name;
        this.price = price;
        this.currency = currency;
        this.availableQuantity = stockQuantity;
        this.reservedQuantity = 0;
    }

    public void reserve(int quantity) {
        if (quantity <= 0) {
            throw new DomainException("Quantity must be positive");
        }
        if (availableQuantity < quantity) {
            throw new DomainException("Insufficient inventory for SKU " + sku);
        }
        availableQuantity -= quantity;
        reservedQuantity += quantity;
    }

    public void captureReserved(int quantity) {
        if (reservedQuantity < quantity) {
            throw new DomainException("Reserved inventory is lower than requested capture for SKU " + sku);
        }
        reservedQuantity -= quantity;
    }

    public void releaseReserved(int quantity) {
        if (reservedQuantity < quantity) {
            throw new DomainException("Reserved inventory is lower than requested release for SKU " + sku);
        }
        reservedQuantity -= quantity;
        availableQuantity += quantity;
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public String getSku() {
        return sku;
    }

    public String getName() {
        return name;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public String getCurrency() {
        return currency;
    }

    public int getAvailableQuantity() {
        return availableQuantity;
    }

    public int getReservedQuantity() {
        return reservedQuantity;
    }
}
