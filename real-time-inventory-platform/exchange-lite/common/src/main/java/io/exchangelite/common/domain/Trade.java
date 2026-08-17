package io.exchangelite.common.domain;

public record Trade(
        String market,
        String buyOrderId,
        String sellOrderId,
        long priceTicks,
        int quantity,
        long executedAtNanos
) {
    public Trade {
        if (market == null || market.isBlank()) {
            throw new IllegalArgumentException("market is required");
        }
        if (buyOrderId == null || buyOrderId.isBlank()) {
            throw new IllegalArgumentException("buyOrderId is required");
        }
        if (sellOrderId == null || sellOrderId.isBlank()) {
            throw new IllegalArgumentException("sellOrderId is required");
        }
        if (priceTicks <= 0) {
            throw new IllegalArgumentException("priceTicks must be positive");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("quantity must be positive");
        }
    }
}
