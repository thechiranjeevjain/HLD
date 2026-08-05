package io.exchangelite.common.domain;

import java.util.List;

public record ExecutionReport(
        String market,
        String clientOrderId,
        String accountId,
        OrderStatus status,
        int filledQuantity,
        int remainingQuantity,
        long averagePriceTicks,
        String reason,
        List<Trade> trades
) {
    public ExecutionReport {
        if (market == null || market.isBlank()) {
            throw new IllegalArgumentException("market is required");
        }
        if (clientOrderId == null || clientOrderId.isBlank()) {
            throw new IllegalArgumentException("clientOrderId is required");
        }
        if (accountId == null || accountId.isBlank()) {
            throw new IllegalArgumentException("accountId is required");
        }
        if (status == null) {
            throw new IllegalArgumentException("status is required");
        }
        if (filledQuantity < 0 || remainingQuantity < 0) {
            throw new IllegalArgumentException("quantities cannot be negative");
        }
        if (averagePriceTicks < 0) {
            throw new IllegalArgumentException("averagePriceTicks cannot be negative");
        }
        reason = reason == null ? "" : reason;
        trades = List.copyOf(trades == null ? List.of() : trades);
    }

    public static ExecutionReport rejected(OrderRequest request, String reason) {
        return new ExecutionReport(
                request.market(),
                request.clientOrderId(),
                request.accountId(),
                OrderStatus.REJECTED,
                0,
                request.quantity(),
                0,
                reason,
                List.of()
        );
    }

    public static ExecutionReport cancelled(CancelRequest request) {
        return new ExecutionReport(
                request.market(),
                request.clientOrderId(),
                request.accountId(),
                OrderStatus.CANCELLED,
                0,
                0,
                0,
                "cancel accepted",
                List.of()
        );
    }
}
