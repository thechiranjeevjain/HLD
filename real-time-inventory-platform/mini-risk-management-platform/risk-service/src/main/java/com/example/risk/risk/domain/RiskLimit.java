package com.example.risk.risk.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(
        name = "risk_limits",
        uniqueConstraints = @UniqueConstraint(name = "uk_risk_limits_client_symbol", columnNames = {"client_id", "symbol"})
)
public class RiskLimit {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String clientId;

    @Column(nullable = false)
    private String symbol;

    @Column(nullable = false)
    private long maxOrderQuantity;

    @Column(nullable = false)
    private long maxPositionQuantity;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal maxDailyExposure;

    protected RiskLimit() {
    }

    public RiskLimit(String clientId, String symbol, long maxOrderQuantity, long maxPositionQuantity, BigDecimal maxDailyExposure) {
        this.clientId = clientId;
        this.symbol = symbol;
        this.maxOrderQuantity = maxOrderQuantity;
        this.maxPositionQuantity = maxPositionQuantity;
        this.maxDailyExposure = maxDailyExposure;
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

    public long getMaxOrderQuantity() {
        return maxOrderQuantity;
    }

    public long getMaxPositionQuantity() {
        return maxPositionQuantity;
    }

    public BigDecimal getMaxDailyExposure() {
        return maxDailyExposure;
    }
}

