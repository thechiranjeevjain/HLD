package com.techstudy.trading;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

/** A single-process vertical slice whose boundaries map directly to deployable production services. */
public final class ElectronicTradingPlatform {
    public enum Side { BUY(1), SELL(-1); final int sign; Side(int sign) { this.sign = sign; } }
    public enum VenueOutcome { ACK_AND_FILL, REJECT, DISCONNECT_AFTER_WRITE }
    public enum OrderState { RISK_REJECTED, VENUE_REJECTED, UNKNOWN, FILLED }

    public record OrderRequest(String clientOrderId, String account, String symbol, Side side, long quantity) {
        public OrderRequest {
            Objects.requireNonNull(clientOrderId); Objects.requireNonNull(account); Objects.requireNonNull(symbol); Objects.requireNonNull(side);
            if (quantity <= 0) throw new IllegalArgumentException("quantity must be positive");
        }
    }
    public record OrderView(String clientOrderId, OrderState state, long priceTicks, String detail) {}
    public record Quote(long priceTicks, Instant receivedAt) {}
    public record TradingEvent(long offset, String type, OrderRequest order, long priceTicks, String detail) {}

    public static final class MarketDataService {
        private final Map<String, Quote> quotes = new HashMap<>();
        public synchronized void onQuote(String symbol, long priceTicks, Instant receivedAt) { quotes.put(symbol, new Quote(priceTicks, receivedAt)); }
        public synchronized Optional<Quote> quote(String symbol) { return Optional.ofNullable(quotes.get(symbol)); }
    }

    public static final class PositionService {
        private final Map<String, Long> positions = new HashMap<>();
        public synchronized void applyFill(OrderRequest order) { positions.merge(key(order.account(), order.symbol()), order.side().sign * order.quantity(), Long::sum); }
        public synchronized long position(String account, String symbol) { return positions.getOrDefault(key(account, symbol), 0L); }
        public synchronized void clear() { positions.clear(); }
        private static String key(String account, String symbol) { return account + "|" + symbol; }
    }

    public static final class RiskService {
        private final long maxAbsolutePosition;
        private final long maxOrderNotionalTicks;
        private final Map<String, Long> pending = new HashMap<>();

        public RiskService(long maxAbsolutePosition, long maxOrderNotionalTicks) {
            this.maxAbsolutePosition = maxAbsolutePosition; this.maxOrderNotionalTicks = maxOrderNotionalTicks;
        }

        public synchronized Optional<String> reserve(OrderRequest order, long priceTicks, long currentPosition) {
            long notional;
            try { notional = Math.multiplyExact(order.quantity(), priceTicks); }
            catch (ArithmeticException e) { return Optional.of("notional overflow"); }
            if (notional > maxOrderNotionalTicks) return Optional.of("max order notional exceeded");
            String key = PositionService.key(order.account(), order.symbol());
            long signedQuantity = order.side().sign * order.quantity();
            long projected = currentPosition + pending.getOrDefault(key, 0L) + signedQuantity;
            if (Math.abs(projected) > maxAbsolutePosition) return Optional.of("position limit exceeded");
            pending.merge(key, signedQuantity, Long::sum);
            return Optional.empty();
        }

        public synchronized void release(OrderRequest order) {
            String key = PositionService.key(order.account(), order.symbol());
            long remaining = pending.getOrDefault(key, 0L) - order.side().sign * order.quantity();
            if (remaining == 0) pending.remove(key); else pending.put(key, remaining);
        }
        public synchronized long pending(String account) {
            String prefix = account + "|";
            return pending.entrySet().stream().filter(e -> e.getKey().startsWith(prefix)).mapToLong(e -> Math.abs(e.getValue())).sum();
        }
        public synchronized void clear() { pending.clear(); }
    }

    public static final class OmsService {
        private final Map<String, OrderView> orders = new LinkedHashMap<>();
        public synchronized Optional<OrderView> find(String id) { return Optional.ofNullable(orders.get(id)); }
        public synchronized void save(OrderView view) { orders.put(view.clientOrderId(), view); }
        public synchronized Map<String, OrderView> snapshot() { return Map.copyOf(orders); }
        public synchronized void clear() { orders.clear(); }
    }

    public static final class ConnectivityService {
        private final AtomicLong messages = new AtomicLong();
        public VenueOutcome route(OrderRequest order, VenueOutcome simulatedOutcome) { messages.incrementAndGet(); return simulatedOutcome; }
        public long messagesSent() { return messages.get(); }
    }

    private final Clock clock;
    private final MarketDataService marketData = new MarketDataService();
    private final PositionService positions = new PositionService();
    private final RiskService risk;
    private final OmsService oms = new OmsService();
    private final ConnectivityService connectivity = new ConnectivityService();
    private final List<TradingEvent> journal;
    private final Map<String, Long> metrics = new HashMap<>();
    private final Duration maxQuoteAge = Duration.ofSeconds(2);

    public ElectronicTradingPlatform(long maxPosition, long maxNotionalTicks, Clock clock, List<TradingEvent> journal) {
        this.risk = new RiskService(maxPosition, maxNotionalTicks);
        this.clock = clock;
        this.journal = journal;
    }

    public synchronized OrderView submit(String apiKey, OrderRequest order, VenueOutcome venueOutcome) {
        if (!"demo-key".equals(apiKey)) throw new SecurityException("gateway authentication failed");
        Optional<OrderView> duplicate = oms.find(order.clientOrderId());
        if (duplicate.isPresent()) { increment("duplicates"); return duplicate.get(); }

        Quote quote = marketData.quote(order.symbol()).orElse(null);
        if (quote == null || Duration.between(quote.receivedAt(), clock.instant()).compareTo(maxQuoteAge) > 0) {
            return terminal(order, OrderState.RISK_REJECTED, quote == null ? 0 : quote.priceTicks(), "missing or stale market data");
        }
        Optional<String> rejection = risk.reserve(order, quote.priceTicks(), positions.position(order.account(), order.symbol()));
        if (rejection.isPresent()) return terminal(order, OrderState.RISK_REJECTED, quote.priceTicks(), rejection.get());

        VenueOutcome outcome = connectivity.route(order, venueOutcome);
        return switch (outcome) {
            case ACK_AND_FILL -> fill(order, quote.priceTicks(), "execution received");
            case REJECT -> { risk.release(order); yield terminal(order, OrderState.VENUE_REJECTED, quote.priceTicks(), "venue rejected"); }
            case DISCONNECT_AFTER_WRITE -> terminal(order, OrderState.UNKNOWN, quote.priceTicks(), "venue outcome uncertain; reservation retained");
        };
    }

    public synchronized OrderView reconcileUnknown(String orderId, boolean venueAccepted) {
        OrderView current = oms.find(orderId).orElseThrow();
        if (current.state() != OrderState.UNKNOWN) return current;
        OrderRequest order = journal.stream().filter(e -> e.order().clientOrderId().equals(orderId)).findFirst().orElseThrow().order();
        if (venueAccepted) return fill(order, current.priceTicks(), "drop-copy reconciliation confirmed fill");
        risk.release(order);
        return terminal(order, OrderState.VENUE_REJECTED, current.priceTicks(), "status query confirmed not accepted");
    }

    private OrderView fill(OrderRequest order, long price, String detail) {
        positions.applyFill(order); risk.release(order);
        return terminal(order, OrderState.FILLED, price, detail);
    }

    private OrderView terminal(OrderRequest order, OrderState state, long price, String detail) {
        OrderView view = new OrderView(order.clientOrderId(), state, price, detail);
        oms.save(view);
        journal.add(new TradingEvent(journal.size() + 1L, state.name(), order, price, detail));
        increment(state.name().toLowerCase());
        return view;
    }

    public synchronized void recoverFromJournal() {
        oms.clear();
        positions.clear();
        risk.clear();
        // In production snapshots bound replay time; this small demo intentionally replays the full event log.
        Map<String, TradingEvent> last = new LinkedHashMap<>();
        journal.forEach(e -> last.put(e.order().clientOrderId(), e));
        last.values().forEach(e -> {
            oms.save(new OrderView(e.order().clientOrderId(), OrderState.valueOf(e.type()), e.priceTicks(), e.detail()));
            if (OrderState.valueOf(e.type()) == OrderState.FILLED) positions.applyFill(e.order());
            if (OrderState.valueOf(e.type()) == OrderState.UNKNOWN)
                risk.reserve(e.order(), e.priceTicks(), positions.position(e.order().account(), e.order().symbol()));
        });
    }

    private void increment(String name) { metrics.merge(name, 1L, Long::sum); }
    public MarketDataService marketData() { return marketData; }
    public PositionService positions() { return positions; }
    public RiskService risk() { return risk; }
    public OmsService oms() { return oms; }
    public ConnectivityService connectivity() { return connectivity; }
    public synchronized Map<String, Long> metrics() { return Map.copyOf(metrics); }

    public static void main(String[] args) {
        List<TradingEvent> journal = new ArrayList<>();
        Clock clock = Clock.systemUTC();
        ElectronicTradingPlatform platform = new ElectronicTradingPlatform(1_000, 50_000_000, clock, journal);
        platform.marketData().onQuote("AAPL", 19_500, clock.instant());
        System.out.println(platform.submit("demo-key", new OrderRequest("C-1", "ACC-7", "AAPL", Side.BUY, 100), VenueOutcome.ACK_AND_FILL));
        System.out.println(platform.submit("demo-key", new OrderRequest("C-2", "ACC-7", "AAPL", Side.SELL, 20), VenueOutcome.DISCONNECT_AFTER_WRITE));
        System.out.println(platform.reconcileUnknown("C-2", true));
        System.out.println("position=" + platform.positions().position("ACC-7", "AAPL") + ", metrics=" + platform.metrics());

        ElectronicTradingPlatform recovered = new ElectronicTradingPlatform(1_000, 50_000_000, clock, journal);
        recovered.recoverFromJournal();
        System.out.println("recovered orders=" + recovered.oms().snapshot() + ", position=" + recovered.positions().position("ACC-7", "AAPL"));
    }
}
