package io.exchangelite.engine.core;

import io.exchangelite.common.domain.CancelRequest;
import io.exchangelite.common.domain.ExecutionReport;
import io.exchangelite.common.domain.OrderRequest;
import io.exchangelite.common.domain.OrderSide;
import io.exchangelite.common.domain.OrderStatus;
import io.exchangelite.common.domain.OrderType;
import io.exchangelite.common.domain.Trade;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

public final class OrderBook {
    private final String market;
    private final NavigableMap<Long, ArrayDeque<Order>> bids = new TreeMap<>(Comparator.reverseOrder());
    private final NavigableMap<Long, ArrayDeque<Order>> asks = new TreeMap<>();
    private final Map<String, Order> openOrdersByClientId = new HashMap<>();

    public OrderBook(String market) {
        this.market = market;
    }

    public synchronized ExecutionReport submit(OrderRequest request, long sequence) {
        if (openOrdersByClientId.containsKey(request.clientOrderId())) {
            return ExecutionReport.rejected(request, "duplicate open clientOrderId");
        }

        Order aggressive = Order.from(request, sequence);
        List<Trade> trades = new ArrayList<>();
        long totalNotional = 0L;

        while (aggressive.remainingQuantity() > 0) {
            Map.Entry<Long, ArrayDeque<Order>> bestLevel = bestContraLevel(aggressive);
            if (bestLevel == null) {
                break;
            }

            ArrayDeque<Order> queue = bestLevel.getValue();
            Order resting = queue.peekFirst();
            int tradeQuantity = Math.min(aggressive.remainingQuantity(), resting.remainingQuantity());
            long tradePrice = resting.priceTicks();
            aggressive.fill(tradeQuantity);
            resting.fill(tradeQuantity);
            totalNotional += tradePrice * tradeQuantity;

            String buyOrderId = aggressive.side() == OrderSide.BUY ? aggressive.clientOrderId() : resting.clientOrderId();
            String sellOrderId = aggressive.side() == OrderSide.SELL ? aggressive.clientOrderId() : resting.clientOrderId();
            trades.add(new Trade(market, buyOrderId, sellOrderId, tradePrice, tradeQuantity, System.nanoTime()));

            if (resting.isFilled()) {
                queue.removeFirst();
                openOrdersByClientId.remove(resting.clientOrderId());
                if (queue.isEmpty()) {
                    contraBook(aggressive).remove(bestLevel.getKey());
                }
            }
        }

        if (aggressive.remainingQuantity() > 0 && aggressive.type() == OrderType.LIMIT) {
            rest(aggressive);
        }

        int filledQuantity = request.quantity() - aggressive.remainingQuantity();
        long averagePrice = filledQuantity == 0 ? 0 : totalNotional / filledQuantity;
        OrderStatus status = statusFor(request, aggressive, filledQuantity);
        String reason = reasonFor(request, aggressive, filledQuantity);
        int reportedRemaining = request.type() == OrderType.MARKET && filledQuantity > 0
                ? aggressive.remainingQuantity()
                : aggressive.remainingQuantity();

        return new ExecutionReport(
                request.market(),
                request.clientOrderId(),
                request.accountId(),
                status,
                filledQuantity,
                reportedRemaining,
                averagePrice,
                reason,
                trades
        );
    }

    public synchronized ExecutionReport cancel(CancelRequest request) {
        Order order = openOrdersByClientId.remove(request.clientOrderId());
        if (order == null) {
            return new ExecutionReport(
                    request.market(),
                    request.clientOrderId(),
                    request.accountId(),
                    OrderStatus.REJECTED,
                    0,
                    0,
                    0,
                    "order not found",
                    List.of()
            );
        }
        if (!order.accountId().equals(request.accountId())) {
            openOrdersByClientId.put(order.clientOrderId(), order);
            return new ExecutionReport(
                    request.market(),
                    request.clientOrderId(),
                    request.accountId(),
                    OrderStatus.REJECTED,
                    0,
                    order.remainingQuantity(),
                    0,
                    "account does not own order",
                    List.of()
            );
        }
        NavigableMap<Long, ArrayDeque<Order>> book = order.side() == OrderSide.BUY ? bids : asks;
        ArrayDeque<Order> level = book.get(order.priceTicks());
        if (level != null) {
            level.removeIf(candidate -> candidate.clientOrderId().equals(order.clientOrderId()));
            if (level.isEmpty()) {
                book.remove(order.priceTicks());
            }
        }
        return ExecutionReport.cancelled(request);
    }

    public synchronized OrderBookSnapshot snapshot(int depth) {
        return new OrderBookSnapshot(
                market,
                levels(bids, depth),
                levels(asks, depth),
                openOrdersByClientId.size()
        );
    }

    public synchronized List<Order> openOrders() {
        return List.copyOf(openOrdersByClientId.values());
    }

    public synchronized int openOrderCount() {
        return openOrdersByClientId.size();
    }

    private void rest(Order order) {
        NavigableMap<Long, ArrayDeque<Order>> book = order.side() == OrderSide.BUY ? bids : asks;
        book.computeIfAbsent(order.priceTicks(), ignored -> new ArrayDeque<>()).addLast(order);
        openOrdersByClientId.put(order.clientOrderId(), order);
    }

    private Map.Entry<Long, ArrayDeque<Order>> bestContraLevel(Order aggressive) {
        NavigableMap<Long, ArrayDeque<Order>> contra = contraBook(aggressive);
        if (contra.isEmpty()) {
            return null;
        }
        Map.Entry<Long, ArrayDeque<Order>> best = contra.firstEntry();
        if (aggressive.type() == OrderType.MARKET) {
            return best;
        }
        boolean crosses = aggressive.side() == OrderSide.BUY
                ? aggressive.priceTicks() >= best.getKey()
                : aggressive.priceTicks() <= best.getKey();
        return crosses ? best : null;
    }

    private NavigableMap<Long, ArrayDeque<Order>> contraBook(Order aggressive) {
        return aggressive.side() == OrderSide.BUY ? asks : bids;
    }

    private OrderStatus statusFor(OrderRequest request, Order aggressive, int filledQuantity) {
        if (filledQuantity == request.quantity()) {
            return OrderStatus.FILLED;
        }
        if (filledQuantity > 0) {
            return OrderStatus.PARTIALLY_FILLED;
        }
        if (request.type() == OrderType.MARKET) {
            return OrderStatus.REJECTED;
        }
        return OrderStatus.ACCEPTED;
    }

    private String reasonFor(OrderRequest request, Order aggressive, int filledQuantity) {
        if (request.type() == OrderType.MARKET && aggressive.remainingQuantity() > 0 && filledQuantity > 0) {
            return "market residual cancelled";
        }
        if (request.type() == OrderType.MARKET && filledQuantity == 0) {
            return "no liquidity";
        }
        return "accepted";
    }

    private List<OrderBookLevel> levels(NavigableMap<Long, ArrayDeque<Order>> book, int depth) {
        List<OrderBookLevel> levels = new ArrayList<>();
        int remaining = Math.max(0, depth);
        for (Map.Entry<Long, ArrayDeque<Order>> entry : book.entrySet()) {
            if (remaining-- == 0) {
                break;
            }
            int visibleQuantity = entry.getValue().stream().mapToInt(Order::remainingQuantity).sum();
            levels.add(new OrderBookLevel(entry.getKey(), visibleQuantity, entry.getValue().size()));
        }
        return levels;
    }
}
