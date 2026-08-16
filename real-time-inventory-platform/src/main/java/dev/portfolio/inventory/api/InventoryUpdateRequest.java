package dev.portfolio.inventory.api;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;

public record InventoryUpdateRequest(
        @NotBlank String updateId,
        @NotBlank String sku,
        @NotBlank String storeId,
        @Min(0) long quantity,
        @Min(0) long version,
        @NotNull Instant eventTime) {}
