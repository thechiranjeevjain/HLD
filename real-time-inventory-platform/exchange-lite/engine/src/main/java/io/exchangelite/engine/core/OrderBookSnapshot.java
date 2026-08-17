package io.exchangelite.engine.core;

import java.util.List;

public record OrderBookSnapshot(
        String market,
        List<OrderBookLevel> bids,
        List<OrderBookLevel> asks,
        int openOrderCount
) {
    public OrderBookSnapshot {
        bids = List.copyOf(bids);
        asks = List.copyOf(asks);
    }
}
