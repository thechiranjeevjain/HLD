package com.interview.fraud.transaction;
import java.util.*; import org.springframework.data.jpa.repository.JpaRepository;
public interface TransactionRepository extends JpaRepository<TransactionEntity,UUID>{Optional<TransactionEntity> findByExternalId(String id); long countByCustomerId(String customerId);}
