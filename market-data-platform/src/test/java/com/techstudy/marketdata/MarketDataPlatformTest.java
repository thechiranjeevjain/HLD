package com.techstudy.marketdata;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;

import static com.techstudy.marketdata.MarketDataPlatform.*;
import static org.junit.jupiter.api.Assertions.*;

class MarketDataPlatformTest {
    @Test
    void repairsGapBeforeApplyingLaterPacket() {
        SequencedFeedHandler handler = new SequencedFeedHandler();
        FeedPacket one = packet(1, "one", Side.BID, 100, 10);
        FeedPacket two = packet(2, "two", Side.ASK, 101, 20);
        FeedPacket three = packet(3, "three", Side.BID, 99, 5);
        assertEquals(1, handler.onPacket(one, ignored -> Optional.empty()).size());
        var recovered = handler.onPacket(three, seq -> Optional.ofNullable(Map.of(2L, two).get(seq)));
        assertEquals(java.util.List.of(two, three), recovered);
        assertEquals(4, handler.expectedSequence());
        assertEquals(1, handler.gapsDetected());
    }

    @Test
    void reconstructsPriceLevelsAndReductions() {
        OrderBook book = new OrderBook("AAPL");
        Normalizer n = new Normalizer();
        book.apply(n.normalize(packet(1, "a", Side.BID, 100, 10)));
        book.apply(n.normalize(packet(2, "b", Side.BID, 100, 15)));
        book.apply(n.normalize(packet(3, "c", Side.ASK, 102, 8)));
        book.apply(new NormalizedEvent(4, "AAPL", EventType.REDUCE, "a", Side.BID, 100, 4));
        book.apply(new NormalizedEvent(5, "AAPL", EventType.TRADE, "b", Side.BID, 100, 5));
        BookSnapshot snapshot = book.snapshot(5);
        assertEquals(16, snapshot.bids().get(0).quantity());
        assertEquals(8, snapshot.asks().get(0).quantity());
    }

    @Test
    void isolatesSlowConsumersByPolicy() {
        FanOutHub hub = new FanOutHub();
        Subscriber conflated = hub.subscribe("ui", 1, SlowConsumerPolicy.CONFLATE_LATEST);
        Subscriber disconnected = hub.subscribe("raw", 1, SlowConsumerPolicy.DISCONNECT);
        BookSnapshot one = new BookSnapshot("AAPL", 1, java.util.List.of(), java.util.List.of());
        BookSnapshot two = new BookSnapshot("AAPL", 2, java.util.List.of(), java.util.List.of());
        hub.publish(one); hub.publish(two);
        assertEquals(2, conflated.poll().orElseThrow().sequence());
        assertEquals(1, conflated.dropped());
        assertFalse(disconnected.connected());
    }

    private FeedPacket packet(long seq, String id, Side side, long price, long qty) {
        return new FeedPacket(seq, "AAPL", EventType.ADD, id, side, price, qty);
    }
}
