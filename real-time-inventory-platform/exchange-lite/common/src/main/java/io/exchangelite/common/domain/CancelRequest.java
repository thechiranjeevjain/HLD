package io.exchangelite.common.domain;

import java.util.Locale;

public record CancelRequest(String market, String clientOrderId, String accountId) {
    public CancelRequest {
        market = requireText(market, "market").toUpperCase(Locale.ROOT);
        clientOrderId = requireText(clientOrderId, "clientOrderId");
        accountId = requireText(accountId, "accountId");
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " is required");
        }
        return value.trim();
    }
}
