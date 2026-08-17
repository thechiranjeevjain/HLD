package com.example.capstone.ecommerce.payment;

import java.math.BigDecimal;
import java.util.UUID;

public record PaymentResponse(
        UUID id,
        UUID orderId,
        BigDecimal amount,
        String currency,
        PaymentStatus status,
        String providerReference,
        String failureReason
) {

    public static PaymentResponse from(PaymentRecord record) {
        return new PaymentResponse(
                record.getId(),
                record.getOrderId(),
                record.getAmount(),
                record.getCurrency(),
                record.getStatus(),
                record.getProviderReference(),
                record.getFailureReason()
        );
    }
}
