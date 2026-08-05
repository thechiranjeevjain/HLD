package com.example.risk.order.domain;

import com.example.risk.common.OrderEvent;
import com.example.risk.common.OrderRequest;
import com.example.risk.common.OrderSide;
import com.example.risk.common.OrderStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "orders")
public class OrderEntity {
    @Id
    @Column(name = "order_id", nullable = false)
    private UUID id;

    @Column(name = "client_id", nullable = false)
    private String clientId;

    @Column(nullable = false)
    private String symbol;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderSide side;

    @Column(nullable = false)
    private long quantity;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal price;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal notional;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    @Column(nullable = false)
    private String reason;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected OrderEntity() {
    }

    private OrderEntity(UUID id, OrderRequest request, Instant now) {
        this.id = id;
        this.clientId = request.clientId();
        this.symbol = request.symbol().toUpperCase();
        this.side = request.side();
        this.quantity = request.quantity();
        this.price = request.price();
        this.notional = request.notional();
        this.status = OrderStatus.RECEIVED;
        this.reason = "Submitted";
        this.createdAt = now;
        this.updatedAt = now;
    }

    public static OrderEntity pending(UUID id, OrderRequest request, Instant now) {
        return new OrderEntity(id, request, now);
    }

    public void mark(OrderStatus status, String reason, Instant now) {
        this.status = status;
        this.reason = reason;
        this.updatedAt = now;
    }

    public OrderEvent toEvent() {
        return new OrderEvent(id, clientId, symbol, side, quantity, price, notional, status, reason, updatedAt);
    }

    public UUID getId() {
        return id;
    }

    public String getClientId() {
        return clientId;
    }

    public String getSymbol() {
        return symbol;
    }

    public OrderSide getSide() {
        return side;
    }

    public long getQuantity() {
        return quantity;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public BigDecimal getNotional() {
        return notional;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public String getReason() {
        return reason;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
