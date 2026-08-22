package com.techstudy.marketdata;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;

/** Deterministic simulation of multicast ingestion, gap recovery, books and fan-out. */
public final class MarketDataPlatform {
    public enum Side { BID, ASK }
    public enum EventType { ADD, REDUCE, DELETE, TRADE }
    public enum SlowConsumerPolicy { CONFLATE_LATEST, DISCONNECT }

    public record FeedPacket(long sequence, String symbol, EventType type, String orderId,
                             Side side, long priceTicks, long quantity) {
        public FeedPacket {
            if (sequence <= 0) throw new IllegalArgumentException("sequence must be positive");
            Objects.requireNonNull(symbol); Objects.requireNonNull(type); Objects.requireNonNull(orderId);
        }
    }

    public record NormalizedEvent(long sequence, String symbol, EventType type, String orderId,
                                  Side side, long priceTicks, long quantity) {}
    public record Level(long priceTicks, long quantity) {}
    public record BookSnapshot(String symbol, long sequence, List<Level> bids, List<Level> asks) {}

    @FunctionalInterface
    public interface RecoverySource { Optional<FeedPacket> packet(long sequence); }

    public static final class SequencedFeedHandler {
        private final NavigableMap<Long, FeedPacket> buffered = new TreeMap<>();
        private long expectedSequence = 1;
        private long gapsDetected;
        private long duplicatesIgnored;

        public synchronized List<FeedPacket> onPacket(FeedPacket packet, RecoverySource recovery) {
            if (packet.sequence() < expectedSequence) {
                duplicatesIgnored++;
                return List.of();
            }
            buffered.putIfAbsent(packet.sequence(), packet);
            if (packet.sequence() > expectedSequence) {
                gapsDetected++;
                for (long missing = expectedSequence; missing < packet.sequence(); missing++) {
                    recovery.packet(missing).ifPresent(p -> buffered.putIfAbsent(p.sequence(), p));
                }
            }
            List<FeedPacket> contiguous = new ArrayList<>();
            while (buffered.containsKey(expectedSequence)) {
                contiguous.add(buffered.remove(expectedSequence));
                expectedSequence++;
            }
            return contiguous;
        }

        public synchronized long expectedSequence() { return expectedSequence; }
        public synchronized long gapsDetected() { return gapsDetected; }
        public synchronized long duplicatesIgnored() { return duplicatesIgnored; }
    }

    public static final class Normalizer {
        public NormalizedEvent normalize(FeedPacket packet) {
            return new NormalizedEvent(packet.sequence(), packet.symbol().toUpperCase(), packet.type(),
                    packet.orderId(), packet.side(), packet.priceTicks(), packet.quantity());
        }
    }

    public static final class OrderBook {
        private record RestingOrder(Side side, long priceTicks, long quantity) {}
        private final String symbol;
        private final Map<String, RestingOrder> orders = new HashMap<>();
        private long sequence;

        public OrderBook(String symbol) { this.symbol = symbol; }

        public synchronized void apply(NormalizedEvent event) {
            if (!symbol.equals(event.symbol())) return;
            switch (event.type()) {
                case ADD -> orders.put(event.orderId(), new RestingOrder(event.side(), event.priceTicks(), event.quantity()));
                case DELETE -> orders.remove(event.orderId());
                case REDUCE, TRADE -> {
                    RestingOrder current = orders.get(event.orderId());
                    if (current != null) {
                        long remaining = current.quantity() - event.quantity();
                        if (remaining > 0) orders.put(event.orderId(), new RestingOrder(current.side(), current.priceTicks(), remaining));
                        else orders.remove(event.orderId());
                    }
                }
            }
            sequence = event.sequence();
        }

        public synchronized BookSnapshot snapshot(int depth) {
            Map<Long, Long> bidLevels = aggregate(Side.BID);
            Map<Long, Long> askLevels = aggregate(Side.ASK);
            List<Level> bids = bidLevels.entrySet().stream().sorted(Map.Entry.<Long, Long>comparingByKey().reversed())
                    .limit(depth).map(e -> new Level(e.getKey(), e.getValue())).toList();
            List<Level> asks = askLevels.entrySet().stream().sorted(Map.Entry.comparingByKey())
                    .limit(depth).map(e -> new Level(e.getKey(), e.getValue())).toList();
            return new BookSnapshot(symbol, sequence, bids, asks);
        }

        private Map<Long, Long> aggregate(Side side) {
            Map<Long, Long> levels = new HashMap<>();
            orders.values().stream().filter(o -> o.side() == side)
                    .forEach(o -> levels.merge(o.priceTicks(), o.quantity(), Long::sum));
            return levels;
        }
    }

    public static final class Subscriber {
        private final String id;
        private final int capacity;
        private final SlowConsumerPolicy policy;
        private final Deque<BookSnapshot> queue = new ArrayDeque<>();
        private boolean connected = true;
        private long dropped;

        Subscriber(String id, int capacity, SlowConsumerPolicy policy) {
            this.id = id; this.capacity = capacity; this.policy = policy;
        }

        synchronized void offer(BookSnapshot snapshot) {
            if (!connected) return;
            if (queue.size() < capacity) { queue.addLast(snapshot); return; }
            dropped++;
            if (policy == SlowConsumerPolicy.DISCONNECT) { connected = false; queue.clear(); }
            else { queue.clear(); queue.addLast(snapshot); }
        }

        public synchronized Optional<BookSnapshot> poll() { return Optional.ofNullable(queue.pollFirst()); }
        public synchronized boolean connected() { return connected; }
        public synchronized long dropped() { return dropped; }
        public String id() { return id; }
    }

    public static final class FanOutHub {
        private final Map<String, Subscriber> subscribers = new LinkedHashMap<>();

        public synchronized Subscriber subscribe(String id, int capacity, SlowConsumerPolicy policy) {
            Subscriber subscriber = new Subscriber(id, capacity, policy);
            subscribers.put(id, subscriber);
            return subscriber;
        }

        public synchronized void publish(BookSnapshot snapshot) {
            subscribers.values().forEach(s -> s.offer(snapshot));
        }
    }

    public static void main(String[] args) {
        Map<Long, FeedPacket> retransmission = Map.of(
                2L, new FeedPacket(2, "AAPL", EventType.ADD, "o2", Side.ASK, 19_502, 80));
        List<FeedPacket> wire = List.of(
                new FeedPacket(1, "AAPL", EventType.ADD, "o1", Side.BID, 19_500, 100),
                new FeedPacket(3, "AAPL", EventType.ADD, "o3", Side.BID, 19_499, 40),
                new FeedPacket(4, "AAPL", EventType.REDUCE, "o1", Side.BID, 19_500, 25));

        SequencedFeedHandler feed = new SequencedFeedHandler();
        Normalizer normalizer = new Normalizer();
        OrderBook book = new OrderBook("AAPL");
        FanOutHub fanOut = new FanOutHub();
        Subscriber fast = fanOut.subscribe("strategy-fast", 10, SlowConsumerPolicy.DISCONNECT);
        Subscriber slow = fanOut.subscribe("ui-slow", 1, SlowConsumerPolicy.CONFLATE_LATEST);

        for (FeedPacket packet : wire) {
            for (FeedPacket ordered : feed.onPacket(packet, seq -> Optional.ofNullable(retransmission.get(seq)))) {
                book.apply(normalizer.normalize(ordered));
                fanOut.publish(book.snapshot(5));
            }
        }
        System.out.println("book=" + book.snapshot(5));
        System.out.println("gaps=" + feed.gapsDetected() + ", nextSequence=" + feed.expectedSequence());
        System.out.println("fast first=" + fast.poll().orElseThrow());
        System.out.println("slow latest=" + slow.poll().orElseThrow() + ", conflated=" + slow.dropped());
    }
}
