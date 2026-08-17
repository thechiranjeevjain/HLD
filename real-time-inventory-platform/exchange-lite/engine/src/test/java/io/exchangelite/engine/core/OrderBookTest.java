package io.exchangelite.engine.core;

import io.exchangelite.common.domain.CancelRequest;
import io.exchangelite.common.domain.ExecutionReport;
import io.exchangelite.common.domain.OrderRequest;
import io.exchangelite.common.domain.OrderSide;
import io.exchangelite.common.domain.OrderStatus;
import io.exchangelite.common.domain.OrderType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OrderBookTest {
    @Test
    void restsLimitOrderWhenThereIsNoCross() {
        OrderBook book = new OrderBook("BTC-USD");
        ExecutionReport report = book.submit(limit("B-1", OrderSide.BUY, 100, 10), 1);

        assertEquals(OrderStatus.ACCEPTED, report.status());
        assertEquals(10, report.remainingQuantity());
        assertEquals(1, book.openOrderCount());
        assertEquals(100, book.snapshot(1).bids().get(0).priceTicks());
    }

    @Test
    void matchesAtRestingPriceAndPreservesResidual() {
        OrderBook book = new OrderBook("BTC-USD");
        book.submit(limit("S-1", OrderSide.SELL, 101, 7), 1);
        book.submit(limit("S-2", OrderSide.SELL, 102, 9), 2);

        ExecutionReport report = book.submit(limit("B-1", OrderSide.BUY, 102, 10), 3);

        assertEquals(OrderStatus.FILLED, report.status());
        assertEquals(10, report.filledQuantity());
        assertEquals(0, report.remainingQuantity());
        assertEquals(101, report.trades().get(0).priceTicks());
        assertEquals(102, report.trades().get(1).priceTicks());
        assertEquals(1, book.openOrderCount());
        assertEquals(6, book.snapshot(1).asks().get(0).visibleQuantity());
    }

    @Test
    void cancelsOnlyOpenOwnedOrder() {
        OrderBook book = new OrderBook("BTC-USD");
        book.submit(limit("B-1", OrderSide.BUY, 100, 10), 1);

        ExecutionReport report = book.cancel(new CancelRequest("BTC-USD", "B-1", "acct"));

        assertEquals(OrderStatus.CANCELLED, report.status());
        assertEquals(0, book.openOrderCount());
    }

    private OrderRequest limit(String id, OrderSide side, long price, int quantity) {
        return new OrderRequest("BTC-USD", id, "acct", side, OrderType.LIMIT, price, quantity);
    }
}
