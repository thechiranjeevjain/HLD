package dev.interview.concurrency;

/** Immutable snapshot published through AtomicReference. */
public record RiskState(long netExposureCents, long acceptedOrders, long rejectedOrders, long version) {
    public static RiskState empty() { return new RiskState(0, 0, 0, 0); }
    public RiskState accepted(long delta) { return new RiskState(Math.addExact(netExposureCents, delta), acceptedOrders + 1, rejectedOrders, version + 1); }
    public RiskState rejected() { return new RiskState(netExposureCents, acceptedOrders, rejectedOrders + 1, version + 1); }
}
