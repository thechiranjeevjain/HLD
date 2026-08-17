package io.exchangelite.engine.core;

import io.exchangelite.common.domain.OrderRequest;
import io.exchangelite.common.domain.OrderSide;
import io.exchangelite.common.domain.OrderType;

public final class Order {
    private final String market;
    private final String clientOrderId;
    private final String accountId;
    private final OrderSide side;
    private final OrderType type;
    private final long priceTicks;
    private final int originalQuantity;
    private final long sequence;
    private final long createdAtNanos;
    private int remainingQuantity;

    private Order(OrderRequest request, long sequence, long createdAtNanos) {
        this.market = request.market();
        this.clientOrderId = request.clientOrderId();
        this.accountId = request.accountId();
        this.side = request.side();
        this.type = request.type();
        this.priceTicks = request.priceTicks();
        this.originalQuantity = request.quantity();
        this.remainingQuantity = request.quantity();
        this.sequence = sequence;
        this.createdAtNanos = createdAtNanos;
    }

    public static Order from(OrderRequest request, long sequence) {
        return new Order(request, sequence, System.nanoTime());
    }

    public String market() {
        return market;
    }

    public String clientOrderId() {
        return clientOrderId;
    }

    public String accountId() {
        return accountId;
    }

    public OrderSide side() {
        return side;
    }

    public OrderType type() {
        return type;
    }

    public long priceTicks() {
        return priceTicks;
    }

    public int originalQuantity() {
        return originalQuantity;
    }

    public int remainingQuantity() {
        return remainingQuantity;
    }

    public long sequence() {
        return sequence;
    }

    public long createdAtNanos() {
        return createdAtNanos;
    }

    public void fill(int quantity) {
        if (quantity <= 0 || quantity > remainingQuantity) {
            throw new IllegalArgumentException("invalid fill quantity: " + quantity);
        }
        remainingQuantity -= quantity;
    }

    public boolean isFilled() {
        return remainingQuantity == 0;
    }
}
