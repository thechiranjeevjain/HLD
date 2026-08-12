package dev.interview.concurrency;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/** Runs independent, read-only checks concurrently on the caller-supplied bounded executor. */
public final class AsyncRiskChecks {
    private final Executor executor;
    public AsyncRiskChecks(Executor executor) { this.executor = executor; }

    public CompletableFuture<Boolean> validate(Order order) {
        var symbol = CompletableFuture.supplyAsync(() -> List.of("AAPL", "MSFT", "NVDA", "AMZN").contains(order.symbol()), executor);
        var size = CompletableFuture.supplyAsync(() -> order.quantity() <= 10_000, executor);
        var price = CompletableFuture.supplyAsync(() -> order.priceCents() <= 1_000_000, executor);
        return CompletableFuture.allOf(symbol, size, price).thenApply(ignored -> symbol.join() && size.join() && price.join());
    }
}
