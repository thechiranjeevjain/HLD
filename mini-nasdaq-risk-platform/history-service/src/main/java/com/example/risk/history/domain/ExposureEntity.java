package com.example.risk.history.domain;

import com.example.risk.common.ExposureSummary;
import com.example.risk.common.OrderEvent;
import com.example.risk.common.OrderSide;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "exposures")
public class ExposureEntity {
    @Id
    @Column(name = "exposure_id", nullable = false)
    private UUID id;

    @Column(name = "client_id", nullable = false)
    private String clientId;

    @Column(nullable = false)
    private String symbol;

    @Column(name = "net_quantity", nullable = false)
    private long netQuantity;

    @Column(name = "gross_notional", nullable = false, precision = 19, scale = 4)
    private BigDecimal grossNotional;

    @Column(name = "daily_exposure", nullable = false, precision = 19, scale = 4)
    private BigDecimal dailyExposure;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected ExposureEntity() {
    }

    public ExposureEntity(String clientId, String symbol) {
        this.id = UUID.randomUUID();
        this.clientId = clientId;
        this.symbol = symbol.toUpperCase();
        this.netQuantity = 0;
        this.grossNotional = BigDecimal.ZERO;
        this.dailyExposure = BigDecimal.ZERO;
        this.updatedAt = Instant.now();
    }

    public void apply(OrderEvent event) {
        long signedQuantity = event.side() == OrderSide.BUY ? event.quantity() : -event.quantity();
        this.netQuantity += signedQuantity;
        this.grossNotional = grossNotional.add(event.notional());
        this.dailyExposure = dailyExposure.add(event.notional());
        this.updatedAt = event.occurredAt();
    }

    public ExposureSummary toSummary() {
        return new ExposureSummary(clientId, symbol, netQuantity, grossNotional, dailyExposure);
    }
}
