package dev.portfolio.inventory.algorithms;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/** O(n) expected time and O(number of SKU/store keys) memory. */
public final class LatestInventoryReducer {
    private LatestInventoryReducer() {}
    public record Update(String updateId, String sku, String storeId, long quantity, long version, Instant eventTime) {}
    public record Key(String sku, String storeId) {}

    public static Map<Key, Update> latest(Iterable<Update> updates) {
        Map<Key, Update> result = new HashMap<>();
        for (Update candidate : updates) {
            result.merge(new Key(candidate.sku(), candidate.storeId()), candidate,
                    (current, incoming) -> newer(incoming, current) ? incoming : current);
        }
        return Map.copyOf(result);
    }
    private static boolean newer(Update a, Update b) {
        if (a.version() != b.version()) return a.version() > b.version();
        int time = a.eventTime().compareTo(b.eventTime());
        return time > 0 || (time == 0 && a.updateId().compareTo(b.updateId()) > 0);
    }
}
