package dev.interview.concurrency;

import java.util.Objects;

/** Immutable input: safe to share because its state cannot change after construction. */
public record Order(long id, String clientId, String symbol, Side side, long quantity, long priceCents) {
    public enum Side { BUY, SELL }

    public Order {
        if (id < 0 || quantity <= 0 || priceCents <= 0) throw new IllegalArgumentException("positive id/quantity/price required");
        Objects.requireNonNull(clientId); Objects.requireNonNull(symbol); Objects.requireNonNull(side);
    }

    public long signedNotionalCents() {
        long value = Math.multiplyExact(quantity, priceCents);
        return side == Side.BUY ? value : -value;
    }
}
