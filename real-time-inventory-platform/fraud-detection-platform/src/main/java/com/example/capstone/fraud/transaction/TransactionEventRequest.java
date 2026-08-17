package com.example.capstone.fraud.transaction;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Locale;

public record TransactionEventRequest(
        @NotBlank
        @Size(max = 160)
        String transactionId,

        @NotBlank
        @Size(max = 160)
        String userId,

        @NotNull
        @DecimalMin("0.01")
        @Digits(integer = 12, fraction = 2)
        BigDecimal amount,

        @NotBlank
        @Pattern(regexp = "[A-Z]{3}", message = "must be an ISO 4217 currency code")
        String currency,

        @NotBlank
        @Size(max = 64)
        String merchantCategory,

        @NotBlank
        @Pattern(regexp = "[A-Z]{2}", message = "must be an ISO 3166-1 alpha-2 country code")
        String country,

        @NotBlank
        @Pattern(regexp = "[A-Z]{2}", message = "must be an ISO 3166-1 alpha-2 country code")
        String homeCountry,

        @NotNull
        Boolean cardPresent,

        Instant occurredAt
) {

    public TransactionEvent toEvent() {
        return new TransactionEvent(
                transactionId.trim(),
                userId.trim(),
                amount,
                currency,
                merchantCategory.trim().toUpperCase(Locale.ROOT),
                country,
                homeCountry,
                cardPresent,
                occurredAt == null ? Instant.now() : occurredAt
        );
    }
}
