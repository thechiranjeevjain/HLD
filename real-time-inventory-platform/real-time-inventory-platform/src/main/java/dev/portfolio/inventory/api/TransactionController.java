package dev.portfolio.inventory.api;

import dev.portfolio.inventory.algorithms.TransactionDeduplicator;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.web.bind.annotation.*;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/v1/transactions")
public class TransactionController {
    public record TransactionRequest(@NotBlank String transactionId, @NotBlank String accountId,
        @NotBlank String merchantId, long amountInCents, @NotBlank String currency, @NotNull Instant timestamp) {
        TransactionDeduplicator.Transaction toDomain() {
            return new TransactionDeduplicator.Transaction(transactionId, accountId, merchantId, amountInCents, currency, timestamp);
        }
    }
    @PostMapping("/duplicates")
    public List<List<TransactionDeduplicator.Transaction>> duplicates(
            @RequestParam(defaultValue = "0") @Min(0) long windowSeconds,
            @RequestBody List<@Valid TransactionRequest> requests) {
        List<TransactionDeduplicator.Transaction> transactions = requests.stream().map(TransactionRequest::toDomain).toList();
        return windowSeconds == 0 ? TransactionDeduplicator.exactGroups(transactions)
                : TransactionDeduplicator.withinWindow(transactions, Duration.ofSeconds(windowSeconds));
    }
}
