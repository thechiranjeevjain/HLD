package dev.portfolio.inventory.api;

import java.time.Instant;

public record InventoryView(String sku, String storeId, long quantity, long version,
                            Instant eventTime, String sourceUpdateId) {}
