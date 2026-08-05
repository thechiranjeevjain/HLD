package com.example.risk.history.domain;

import com.example.risk.common.OrderEvent;
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
@Table(name = "order_events")
public class OrderEventEntity {
    @Id
    @Column(name = "order_id", nullable = false)
    private UUID orderId;

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

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    protected OrderEventEntity() {
    }

    private OrderEventEntity(OrderEvent event) {
        this.orderId = event.orderId();
        this.clientId = event.clientId();
        this.symbol = event.symbol().toUpperCase();
        this.side = event.side();
        this.quantity = event.quantity();
        this.price = event.price();
        this.notional = event.notional();
        this.status = event.status();
        this.reason = event.reason();
        this.occurredAt = event.occurredAt();
    }

    public static OrderEventEntity from(OrderEvent event) {
        return new OrderEventEntity(event);
    }
}
