package com.example.capstone.fraud.risk;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FraudDecisionRepository extends JpaRepository<FraudDecision, UUID> {

    Optional<FraudDecision> findByTransactionId(String transactionId);
}
