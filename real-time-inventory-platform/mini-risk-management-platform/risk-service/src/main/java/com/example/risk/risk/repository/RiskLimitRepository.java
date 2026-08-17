package com.example.risk.risk.repository;

import com.example.risk.risk.domain.RiskLimit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface RiskLimitRepository extends JpaRepository<RiskLimit, UUID> {
    Optional<RiskLimit> findByClientIdAndSymbol(String clientId, String symbol);
}

