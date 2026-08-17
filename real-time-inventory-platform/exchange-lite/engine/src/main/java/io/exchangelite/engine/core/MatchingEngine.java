package io.exchangelite.engine.core;

import io.exchangelite.common.domain.CancelRequest;
import io.exchangelite.common.domain.ExecutionReport;
import io.exchangelite.common.domain.OrderRequest;
import io.exchangelite.common.domain.OrderStatus;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public final class MatchingEngine {
    private final AtomicLong sequence = new AtomicLong(1);
    private final Map<String, OrderBook> booksByMarket = new ConcurrentHashMap<>();

    public ExecutionReport submit(OrderRequest request) {
        OrderBook book = booksByMarket.computeIfAbsent(request.market(), OrderBook::new);
        return book.submit(request, sequence.getAndIncrement());
    }

    public ExecutionReport cancel(CancelRequest request) {
        OrderBook book = booksByMarket.get(request.market());
        if (book == null) {
            return new ExecutionReport(
                    request.market(),
                    request.clientOrderId(),
                    request.accountId(),
                    OrderStatus.REJECTED,
                    0,
                    0,
                    0,
                    "market has no book",
                    List.of()
            );
        }
        return book.cancel(request);
    }

    public List<OrderBookSnapshot> snapshots(int depth) {
        return booksByMarket.values().stream()
                .map(book -> book.snapshot(depth))
                .toList();
    }

    public List<Order> openOrders() {
        return booksByMarket.values().stream()
                .flatMap(book -> book.openOrders().stream())
                .toList();
    }

    public int openOrderCount() {
        return booksByMarket.values().stream()
                .mapToInt(OrderBook::openOrderCount)
                .sum();
    }
}
