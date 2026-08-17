package com.example.risk.history.domain;

import com.example.risk.common.OrderSide;
import com.example.risk.common.OrderStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "exposure_events")
public class ExposureEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private UUID orderId;

    @Column(nullable = false)
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
    private Instant occurredAt;

    protected ExposureEvent() {
    }

    public ExposureEvent(UUID orderId, String clientId, String symbol, OrderSide side, long quantity,
                         BigDecimal price, BigDecimal notional, OrderStatus status, Instant occurredAt) {
        this.orderId = orderId;
        this.clientId = clientId;
        this.symbol = symbol;
        this.side = side;
        this.quantity = quantity;
        this.price = price;
        this.notional = notional;
        this.status = status;
        this.occurredAt = occurredAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getOrderId() {
        return orderId;
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

    public Instant getOccurredAt() {
        return occurredAt;
    }
}

