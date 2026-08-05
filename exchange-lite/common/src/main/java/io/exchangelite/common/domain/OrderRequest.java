package io.exchangelite.common.domain;

import java.util.Locale;
import java.util.Objects;

public record OrderRequest(
        String market,
        String clientOrderId,
        String accountId,
        OrderSide side,
        OrderType type,
        long priceTicks,
        int quantity
) {
    public OrderRequest {
        market = requireText(market, "market").toUpperCase(Locale.ROOT);
        clientOrderId = requireText(clientOrderId, "clientOrderId");
        accountId = requireText(accountId, "accountId");
        side = Objects.requireNonNull(side, "side");
        type = Objects.requireNonNull(type, "type");
        if (quantity <= 0) {
            throw new IllegalArgumentException("quantity must be positive");
        }
        if (type == OrderType.LIMIT && priceTicks <= 0) {
            throw new IllegalArgumentException("limit orders require a positive price");
        }
        if (type == OrderType.MARKET && priceTicks < 0) {
            throw new IllegalArgumentException("market order price cannot be negative");
        }
    }

    public long maxNotionalTicks() {
        if (type == OrderType.MARKET) {
            return 0L;
        }
        return Math.multiplyExact(priceTicks, quantity);
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " is required");
        }
        return value.trim();
    }
}
