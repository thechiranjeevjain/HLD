package io.exchangelite.engine.core;

import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class MarketManager {
    private final Set<String> openMarkets = ConcurrentHashMap.newKeySet();

    public MarketManager(Set<String> initialMarkets) {
        if (initialMarkets == null || initialMarkets.isEmpty()) {
            throw new IllegalArgumentException("at least one market is required");
        }
        initialMarkets.forEach(this::open);
    }

    public void open(String market) {
        openMarkets.add(normalize(market));
    }

    public void close(String market) {
        openMarkets.remove(normalize(market));
    }

    public boolean isOpen(String market) {
        return openMarkets.contains(normalize(market));
    }

    public Set<String> openMarkets() {
        return Set.copyOf(openMarkets);
    }

    private String normalize(String market) {
        if (market == null || market.isBlank()) {
            throw new IllegalArgumentException("market is required");
        }
        return market.trim().toUpperCase(Locale.ROOT);
    }
}
