package dev.portfolio.inventory.algorithms;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

public final class TransactionDeduplicator {
    private TransactionDeduplicator() {}
    public record Transaction(String transactionId, String accountId, String merchantId,
                              long amountInCents, String currency, Instant timestamp) {}
    public record Fingerprint(String accountId, String merchantId, long amountInCents, String currency) {}

    /** Exact duplicates by normalized business fields; O(n) expected time. */
    public static List<List<Transaction>> exactGroups(Iterable<Transaction> input) {
        Map<String, List<Transaction>> groups = new LinkedHashMap<>();
        for (Transaction t : input) groups.computeIfAbsent(exactKey(t), ignored -> new ArrayList<>()).add(t);
        return groups.values().stream().filter(g -> g.size() > 1).map(List::copyOf).toList();
    }

    /** Near-duplicate groups. Input may be unordered; O(n log n) due to sorting. */
    public static List<List<Transaction>> withinWindow(Collection<Transaction> input, Duration window) {
        if (window.isNegative()) throw new IllegalArgumentException("window must not be negative");
        Map<Fingerprint, List<Transaction>> buckets = new HashMap<>();
        for (Transaction t : input) buckets.computeIfAbsent(fingerprint(t), ignored -> new ArrayList<>()).add(t);
        List<List<Transaction>> duplicates = new ArrayList<>();
        for (List<Transaction> bucket : buckets.values()) {
            bucket.sort(Comparator.comparing(Transaction::timestamp).thenComparing(Transaction::transactionId));
            List<Transaction> cluster = new ArrayList<>();
            for (Transaction t : bucket) {
                if (cluster.isEmpty() || Duration.between(cluster.get(cluster.size() - 1).timestamp(), t.timestamp()).compareTo(window) <= 0) {
                    cluster.add(t);
                } else {
                    if (cluster.size() > 1) duplicates.add(List.copyOf(cluster));
                    cluster = new ArrayList<>(List.of(t));
                }
            }
            if (cluster.size() > 1) duplicates.add(List.copyOf(cluster));
        }
        return List.copyOf(duplicates);
    }

    private static String exactKey(Transaction t) { return fingerprint(t) + "|" + t.timestamp(); }
    private static Fingerprint fingerprint(Transaction t) {
        return new Fingerprint(normalize(t.accountId()), normalize(t.merchantId()), t.amountInCents(), normalize(t.currency()));
    }
    private static String normalize(String value) { return value.strip().toUpperCase(Locale.ROOT); }
}
