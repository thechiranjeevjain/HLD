package dev.portfolio.inventory.algorithms;

import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

class LatestInventoryReducerTest {
    @Test void keepsLatestVersionDespiteOutOfOrderArrival() {
        var updates = List.of(
                update("u3", 3, 30, "2026-08-16T10:03:00Z"),
                update("u1", 1, 10, "2026-08-16T10:01:00Z"),
                update("u2", 2, 20, "2026-08-16T10:02:00Z"));
        var latest = LatestInventoryReducer.latest(updates);
        assertThat(latest).hasSize(1);
        assertThat(latest.values().iterator().next().quantity()).isEqualTo(30);
    }
    @Test void breaksEqualVersionTieDeterministically() {
        var older = update("u1", 5, 10, "2026-08-16T10:00:00Z");
        var newer = update("u2", 5, 15, "2026-08-16T10:01:00Z");
        assertThat(LatestInventoryReducer.latest(List.of(newer, older)).values().iterator().next()).isEqualTo(newer);
    }
    private LatestInventoryReducer.Update update(String id, long version, long quantity, String time) {
        return new LatestInventoryReducer.Update(id, "SKU-1", "STORE-1", quantity, version, Instant.parse(time));
    }
}
