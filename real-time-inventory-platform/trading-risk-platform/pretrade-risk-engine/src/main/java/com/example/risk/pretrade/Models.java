package com.example.risk.pretrade;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public final class Models {
    private Models() {
    }

    public enum Side {
        BUY,
        SELL;

        public static Side fromFix(String value) {
            if ("1".equals(value)) {
                return BUY;
            }
            if ("2".equals(value)) {
                return SELL;
            }
            throw new IllegalArgumentException("Unsupported FIX side 54=" + value);
        }
    }

    public enum OrderStatus {
        ACCEPTED,
        REJECTED,
        FILLED
    }

    public enum EventType {
        ENGINE_RESET,
        ORDER_ACCEPTED,
        ORDER_REJECTED,
        EXPOSURE_RESERVED,
        FILL_APPLIED,
        MARKET_PRICE_CHANGED,
        KILL_SWITCH_CHANGED,
        CIRCUIT_BREAKER_CHANGED
    }

    public record OrderRequest(
            @NotBlank String clOrdId,
            @NotBlank String account,
            @NotBlank String symbol,
            @NotNull Side side,
            @Positive long quantity,
            @NotNull @DecimalMin("0.01") BigDecimal price,
            boolean autoFill
    ) {
        public OrderRequest {
            clOrdId = blankToGenerated(clOrdId);
            account = normalizeRequired(account, "account");
            symbol = normalizeRequired(symbol, "symbol");
            if (price != null) {
                price = money(price);
            }
        }

        public BigDecimal notional() {
            return money(price.multiply(BigDecimal.valueOf(quantity)));
        }
    }

    public record FixOrderRequest(@NotBlank String message) {
    }

    public record MarketPriceRequest(
            @NotBlank String symbol,
            @NotNull @DecimalMin("0.01") BigDecimal price
    ) {
        public MarketPriceRequest {
            symbol = normalizeRequired(symbol, "symbol");
            if (price != null) {
                price = money(price);
            }
        }
    }

    public record KillSwitchRequest(
            @NotBlank String scope,
            String key,
            boolean enabled,
            String reason
    ) {
        public KillSwitchRequest {
            scope = normalizeRequired(scope, "scope");
            key = key == null || key.isBlank() ? "*" : key.trim().toUpperCase(Locale.ROOT);
            reason = reason == null || reason.isBlank() ? "operator request" : reason.trim();
        }
    }

    public record CircuitBreakerRequest(boolean open, String reason) {
        public CircuitBreakerRequest {
            reason = reason == null || reason.isBlank() ? "operator request" : reason.trim();
        }
    }

    public record RiskCheckResult(String name, boolean passed, String detail, long latencyMicros) {
    }

    public record OrderDecision(
            String orderId,
            String clOrdId,
            String account,
            String symbol,
            Side side,
            long quantity,
            BigDecimal price,
            BigDecimal notional,
            OrderStatus status,
            String reason,
            List<RiskCheckResult> checks,
            long totalLatencyMicros,
            Instant completedAt
    ) {
    }

    public record MarketPrice(String symbol, BigDecimal price, Instant updatedAt) {
    }

    public record PositionSnapshot(
            String account,
            String symbol,
            long netQuantity,
            long openBuyQuantity,
            long openSellQuantity,
            BigDecimal reservedNotional,
            BigDecimal averageCost,
            BigDecimal marketPrice,
            BigDecimal realizedPnl,
            BigDecimal unrealizedPnl
    ) {
    }

    public record AccountSummary(
            String account,
            BigDecimal buyingPowerLimit,
            BigDecimal reservedNotional,
            BigDecimal realizedPnl,
            BigDecimal unrealizedPnl,
            BigDecimal availableBuyingPower
    ) {
    }

    public record AuditEvent(
            long sequence,
            Instant occurredAt,
            EventType type,
            String aggregateKey,
            String message,
            Map<String, Object> data
    ) {
    }

    public record LimitsSnapshot(
            BigDecimal accountBuyingPower,
            long maxOrderQuantity,
            BigDecimal maxOrderNotional,
            long positionLimit,
            BigDecimal priceCollarPercent,
            long marketDataStaleAfterSeconds
    ) {
    }

    public record EngineState(
            Map<String, AccountSummary> accounts,
            List<PositionSnapshot> positions,
            Map<String, MarketPrice> marketData,
            Map<String, Boolean> killSwitches,
            boolean circuitBreakerOpen,
            List<OrderDecision> recentDecisions,
            List<AuditEvent> auditTrail,
            LimitsSnapshot limits
    ) {
    }

    public record ScenarioResult(
            String name,
            String talkingPoint,
            List<OrderDecision> decisions,
            EngineState state
    ) {
    }

    public record ApiError(String message) {
    }

    private static String blankToGenerated(String value) {
        if (value == null || value.isBlank()) {
            return "DEMO-" + UUID.randomUUID();
        }
        return value.trim();
    }

    private static String normalizeRequired(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }

    public static BigDecimal money(BigDecimal value) {
        return value.setScale(4, RoundingMode.HALF_UP);
    }
}
