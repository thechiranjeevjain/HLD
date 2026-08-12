package dev.interview.concurrency;

import java.time.Duration;
import java.util.ArrayList;
import java.util.SplittableRandom;
import java.util.concurrent.CompletableFuture;

/** CLI entry point. Default mode safely submits exactly 100,000 deterministic orders. */
public final class LoadGenerator {
    public static void main(String[] args) throws Exception {
        if (args.length > 0 && args[0].equalsIgnoreCase("failures")) { FailureDemos.printAll(); return; }
        int count = args.length > 1 && args[0].equalsIgnoreCase("safe") ? Integer.parseInt(args[1]) : 100_000;
        int cores = Runtime.getRuntime().availableProcessors();
        try (var processor = new ConcurrentOrderProcessor(Math.max(4, cores), 1_024, 50_000_000L)) {
            var random = new SplittableRandom(42); var futures = new ArrayList<CompletableFuture<ConcurrentOrderProcessor.Result>>(count);
            String[] clients = new String[100]; for (int i = 0; i < clients.length; i++) clients[i] = "CLIENT-" + i;
            String[] symbols = {"AAPL", "MSFT", "NVDA", "AMZN"};
            long started = System.nanoTime();
            for (int i = 0; i < count; i++) {
                Order.Side side = random.nextBoolean() ? Order.Side.BUY : Order.Side.SELL;
                futures.add(processor.submit(new Order(i, clients[random.nextInt(clients.length)], symbols[random.nextInt(symbols.length)], side, random.nextLong(1, 101), random.nextLong(1_000, 50_001))));
            }
            CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).join();
            double seconds = (System.nanoTime() - started) / 1_000_000_000.0;
            System.out.printf("submitted=%d accepted=%d rejected=%d clients=%d time=%.3fs throughput=%.0f orders/s%n", processor.submitted(), processor.accepted(), processor.rejected(), processor.exposureSnapshot().size(), seconds, count / seconds);
            System.out.println("executor=" + processor.monitor().report());
            boolean invariant = processor.exposureSnapshot().values().stream().allMatch(v -> Math.abs(v) <= 50_000_000L);
            System.out.println("exposure-limit-invariant=" + invariant);
            processor.await(Duration.ofSeconds(10));
        }
    }
}
