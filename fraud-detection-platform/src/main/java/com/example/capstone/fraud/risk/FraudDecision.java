package com.example.capstone.fraud.risk;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "fraud_decisions")
public class FraudDecision {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 160)
    private String transactionId;

    @Column(nullable = false, length = 160)
    private String userId;

    @Column(nullable = false)
    private int riskScore;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private RiskLevel riskLevel;

    @Column(nullable = false, length = 2000)
    private String reasons;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected FraudDecision() {
    }

    public FraudDecision(String transactionId, String userId, int riskScore, RiskLevel riskLevel, List<String> reasons) {
        this.transactionId = transactionId;
        this.userId = userId;
        this.riskScore = riskScore;
        this.riskLevel = riskLevel;
        this.reasons = String.join(" | ", reasons);
    }

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public String getUserId() {
        return userId;
    }

    public int getRiskScore() {
        return riskScore;
    }

    public RiskLevel getRiskLevel() {
        return riskLevel;
    }

    public List<String> getReasons() {
        return reasons.isBlank() ? List.of() : List.of(reasons.split("\\s\\|\\s"));
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
