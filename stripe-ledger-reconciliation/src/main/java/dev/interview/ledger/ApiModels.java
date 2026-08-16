package dev.interview.ledger;

import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

final class ApiModels {
  private ApiModels() {}
  record CreatePayment(@NotBlank String customerId, @NotBlank String orderId, @Positive long amount,
                       @Pattern(regexp="[A-Z]{3}") String currency,
                       @Pattern(regexp="[A-Z]{3}") String settlementCurrency,
                       boolean simulateCaptureFailure) {}
  record Webhook(@NotBlank String eventId, @NotBlank String type, @NotBlank String paymentIntentId,
                 String stripeObjectId, long amount, String currency, String reason,
                 Long settlementAmount, String settlementCurrency, String fxRate) {}
  record CreateReconciliation(@NotBlank String source, @NotNull LocalDate rangeStart, @NotNull LocalDate rangeEnd) {}
  record Adjustment(@NotBlank String reconciliationId, @NotBlank String reason, @NotEmpty List<AdjustmentEntry> entries) {}
  record AdjustmentEntry(@NotBlank String accountId, @NotBlank String currency, long amount, @NotBlank String matchKey) {}
  record ImportExternal(@NotBlank String externalId, @NotBlank String matchKey, long amount,
                        @Pattern(regexp="[A-Z]{3}") String currency, @NotBlank String status) {}
  record ExternalFile(@NotBlank String fileName, @NotBlank String schemaVersion, @NotEmpty List<Map<String,Object>> rows) {}
  record ScaleRequest(@Min(1) @Max(1000000) int transactions, @Pattern(regexp="[A-Z]{3}") String currency, @Min(0) @Max(100) int mismatchPercent) {}
}
