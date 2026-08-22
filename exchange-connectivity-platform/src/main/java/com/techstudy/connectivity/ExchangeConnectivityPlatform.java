package com.techstudy.connectivity;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongSupplier;

/**
 * Executable interview-sized model of a venue gateway. It deliberately keeps
 * networking behind the session boundary so sequencing and recovery can be tested
 * deterministically without pretending to be a production FIX/OUCH stack.
 */
public final class ExchangeConnectivityPlatform {
    public enum Protocol { FIX, OUCH }
    public enum Side { BUY, SELL }
    public enum SessionState { DISCONNECTED, LOGON_SENT, CONNECTED, RECOVERING }
    public enum OrderState { SENT, ACKNOWLEDGED, REJECTED, UNKNOWN }
    public enum TransmissionOutcome { ACK, REJECT, DISCONNECT_AFTER_WRITE }
    public enum Direction { OUTBOUND, INBOUND }

    public record OrderIntent(String clientOrderId, String symbol, Side side, long quantity, long priceTicks) {
        public OrderIntent {
            Objects.requireNonNull(clientOrderId);
            Objects.requireNonNull(symbol);
            Objects.requireNonNull(side);
            if (quantity <= 0 || priceTicks <= 0) throw new IllegalArgumentException("quantity and price must be positive");
        }
    }

    public record SendResult(String clientOrderId, OrderState state, long outboundSequence, String detail) {}
    public record JournalEntry(Direction direction, long sequence, String clientOrderId,
                               OrderState state, String detail, Instant at) {}

    public static final class FencingLease {
        private final AtomicLong epoch = new AtomicLong();
        private volatile String owner = "";

        public synchronized long acquire(String candidate) {
            owner = candidate;
            return epoch.incrementAndGet();
        }

        public boolean owns(String candidate, long candidateEpoch) {
            return owner.equals(candidate) && epoch.get() == candidateEpoch;
        }
    }

    public static final class TokenBucket {
        private final long capacity;
        private final double refillPerNano;
        private final LongSupplier nanoTime;
        private double tokens;
        private long lastRefill;

        public TokenBucket(long capacity, long refillPerSecond) {
            this(capacity, refillPerSecond, System::nanoTime);
        }

        TokenBucket(long capacity, long refillPerSecond, LongSupplier nanoTime) {
            if (capacity <= 0 || refillPerSecond <= 0) throw new IllegalArgumentException("rates must be positive");
            this.capacity = capacity;
            this.tokens = capacity;
            this.refillPerNano = refillPerSecond / 1_000_000_000.0;
            this.nanoTime = nanoTime;
            this.lastRefill = nanoTime.getAsLong();
        }

        public synchronized boolean tryAcquire() {
            long now = nanoTime.getAsLong();
            tokens = Math.min(capacity, tokens + Math.max(0, now - lastRefill) * refillPerNano);
            lastRefill = now;
            if (tokens < 1) return false;
            tokens--;
            return true;
        }
    }

    public static final class VenueSession {
        private final String instanceId;
        private final String venue;
        private final Protocol protocol;
        private final TokenBucket throttle;
        private final FencingLease lease;
        private final List<JournalEntry> journal;
        private final Map<String, OrderState> orders = new LinkedHashMap<>();
        private final Map<String, Long> outboundSequences = new LinkedHashMap<>();
        private long leaseEpoch;
        private long nextOutboundSequence = 1;
        private long expectedInboundSequence = 1;
        private SessionState state = SessionState.DISCONNECTED;

        public VenueSession(String instanceId, String venue, Protocol protocol, TokenBucket throttle,
                            FencingLease lease, List<JournalEntry> durableJournal) {
            this.instanceId = Objects.requireNonNull(instanceId);
            this.venue = Objects.requireNonNull(venue);
            this.protocol = Objects.requireNonNull(protocol);
            this.throttle = Objects.requireNonNull(throttle);
            this.lease = Objects.requireNonNull(lease);
            this.journal = Objects.requireNonNull(durableJournal);
            restore(durableJournal);
        }

        public synchronized long promoteAndLogon() {
            state = SessionState.LOGON_SENT;
            leaseEpoch = lease.acquire(instanceId);
            state = SessionState.CONNECTED;
            return leaseEpoch;
        }

        public synchronized SendResult send(OrderIntent order, TransmissionOutcome outcome) {
            OrderState previous = orders.get(order.clientOrderId());
            if (previous != null) {
                return new SendResult(order.clientOrderId(), previous, 0, "duplicate clientOrderId suppressed");
            }
            if (state != SessionState.CONNECTED || !lease.owns(instanceId, leaseEpoch)) {
                return new SendResult(order.clientOrderId(), OrderState.REJECTED, 0, "not active/fenced session");
            }
            if (!throttle.tryAcquire()) {
                return new SendResult(order.clientOrderId(), OrderState.REJECTED, 0, "venue throttle exhausted");
            }

            long sequence = nextOutboundSequence++;
            record(sequence, order.clientOrderId(), OrderState.SENT,
                    protocol + " NewOrderSingle sent to " + venue);

            return switch (outcome) {
                case ACK -> transition(sequence, order.clientOrderId(), OrderState.ACKNOWLEDGED, "venue accepted");
                case REJECT -> transition(sequence, order.clientOrderId(), OrderState.REJECTED, "venue rejected");
                case DISCONNECT_AFTER_WRITE -> {
                    state = SessionState.DISCONNECTED;
                    yield transition(sequence, order.clientOrderId(), OrderState.UNKNOWN,
                            "write may have reached venue; reconcile by status query or drop copy");
                }
            };
        }

        public synchronized String onInbound(long sequence, String clientOrderId, OrderState newState) {
            if (sequence < expectedInboundSequence) return "DUPLICATE_IGNORED:" + sequence;
            if (sequence > expectedInboundSequence) {
                state = SessionState.RECOVERING;
                return "RESEND_REQUEST:" + expectedInboundSequence + "-" + (sequence - 1);
            }
            expectedInboundSequence++;
            orders.put(clientOrderId, newState);
            journal.add(new JournalEntry(Direction.INBOUND, sequence, clientOrderId, newState,
                    "inbound execution report applied", Instant.now()));
            state = SessionState.CONNECTED;
            return "APPLIED:" + sequence;
        }

        public synchronized void reconcileUnknown(String clientOrderId, OrderState venueTruth) {
            if (orders.get(clientOrderId) != OrderState.UNKNOWN) return;
            Long originalSequence = outboundSequences.get(clientOrderId);
            if (originalSequence == null) {
                throw new IllegalStateException("missing outbound sequence for uncertain order " + clientOrderId);
            }
            transition(originalSequence, clientOrderId, venueTruth, "uncertain outcome reconciled");
        }

        private SendResult transition(long sequence, String id, OrderState target, String detail) {
            record(sequence, id, target, detail);
            return new SendResult(id, target, sequence, detail);
        }

        private void record(long sequence, String id, OrderState target, String detail) {
            orders.put(id, target);
            outboundSequences.put(id, sequence);
            journal.add(new JournalEntry(Direction.OUTBOUND, sequence, id, target, detail, Instant.now()));
        }

        private void restore(List<JournalEntry> entries) {
            for (JournalEntry entry : entries) {
                orders.put(entry.clientOrderId(), entry.state());
                if (entry.direction() == Direction.OUTBOUND) {
                    outboundSequences.put(entry.clientOrderId(), entry.sequence());
                    nextOutboundSequence = Math.max(nextOutboundSequence, entry.sequence() + 1);
                } else {
                    expectedInboundSequence = Math.max(expectedInboundSequence, entry.sequence() + 1);
                }
            }
        }

        public synchronized SessionState state() { return state; }
        public synchronized long nextOutboundSequence() { return nextOutboundSequence; }
        public synchronized Map<String, OrderState> orderSnapshot() { return Map.copyOf(orders); }
        public String venue() { return venue; }
        public Protocol protocol() { return protocol; }
    }

    public static void main(String[] args) {
        List<JournalEntry> journal = new ArrayList<>();
        FencingLease lease = new FencingLease();
        VenueSession primary = new VenueSession("gateway-a", "XNAS", Protocol.OUCH,
                new TokenBucket(2, 2), lease, journal);
        primary.promoteAndLogon();

        System.out.println(primary.send(new OrderIntent("C-100", "AAPL", Side.BUY, 100, 19_500),
                TransmissionOutcome.ACK));
        System.out.println(primary.send(new OrderIntent("C-101", "MSFT", Side.SELL, 50, 42_000),
                TransmissionOutcome.DISCONNECT_AFTER_WRITE));

        VenueSession standby = new VenueSession("gateway-b", "XNAS", Protocol.OUCH,
                new TokenBucket(10, 10), lease, journal);
        standby.promoteAndLogon();
        standby.reconcileUnknown("C-101", OrderState.ACKNOWLEDGED);

        System.out.println("failover state=" + standby.state() + ", nextSeq=" + standby.nextOutboundSequence());
        System.out.println("orders=" + standby.orderSnapshot());
        System.out.println("gap handling=" + standby.onInbound(3, "C-102", OrderState.ACKNOWLEDGED));
    }
}
