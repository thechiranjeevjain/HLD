package io.exchangelite.engine.core;

import io.exchangelite.common.domain.OrderRequest;
import io.exchangelite.common.domain.OrderType;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class RiskEngine {
    private final int maxOrderQuantity;
    private final long maxOrderNotionalTicks;
    private final Set<String> blockedAccounts = ConcurrentHashMap.newKeySet();

    public RiskEngine(int maxOrderQuantity, long maxOrderNotionalTicks) {
        if (maxOrderQuantity <= 0) {
            throw new IllegalArgumentException("maxOrderQuantity must be positive");
        }
        if (maxOrderNotionalTicks <= 0) {
            throw new IllegalArgumentException("maxOrderNotionalTicks must be positive");
        }
        this.maxOrderQuantity = maxOrderQuantity;
        this.maxOrderNotionalTicks = maxOrderNotionalTicks;
    }

    public RiskDecision evaluate(OrderRequest request) {
        if (blockedAccounts.contains(request.accountId())) {
            return RiskDecision.rejected("account blocked");
        }
        if (request.quantity() > maxOrderQuantity) {
            return RiskDecision.rejected("quantity exceeds max order quantity");
        }
        if (request.type() == OrderType.LIMIT && request.maxNotionalTicks() > maxOrderNotionalTicks) {
            return RiskDecision.rejected("notional exceeds max order notional");
        }
        return RiskDecision.allow();
    }

    public void blockAccount(String accountId) {
        blockedAccounts.add(accountId);
    }

    public String json() {
        return "{"
                + "\"maxOrderQuantity\":" + maxOrderQuantity + ","
                + "\"maxOrderNotionalTicks\":" + maxOrderNotionalTicks + ","
                + "\"blockedAccounts\":" + blockedAccounts.size()
                + "}";
    }
}
