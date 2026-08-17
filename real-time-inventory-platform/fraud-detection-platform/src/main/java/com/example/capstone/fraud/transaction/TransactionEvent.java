package com.example.capstone.fraud.transaction;

import java.math.BigDecimal;
import java.time.Instant;

public record TransactionEvent(
        String transactionId,
        String userId,
        BigDecimal amount,
        String currency,
        String merchantCategory,
        String country,
        String homeCountry,
        boolean cardPresent,
        Instant occurredAt
) {
}
