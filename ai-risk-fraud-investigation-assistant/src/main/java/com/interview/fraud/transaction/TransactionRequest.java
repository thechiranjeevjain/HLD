package com.interview.fraud.transaction;
import jakarta.validation.constraints.*; import java.math.BigDecimal; import java.time.Instant;
public record TransactionRequest(@NotBlank String transactionId,@NotBlank String customerId,@NotBlank String merchantId,@NotBlank String deviceId,@NotNull @Positive BigDecimal amount,@Pattern(regexp="[A-Z]{3}") String currency,@Pattern(regexp="[A-Z]{2}") String country,Instant occurredAt) {}
