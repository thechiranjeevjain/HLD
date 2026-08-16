package dev.portfolio.inventory.algorithms;

import org.junit.jupiter.api.Test;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

class TransactionDeduplicatorTest {
    @Test void findsExactDuplicatesAfterNormalization() {
        var a = tx("T1", "acct-1", "coffee", "2026-08-16T10:00:00Z");
        var b = tx("T2", " ACCT-1 ", "COFFEE", "2026-08-16T10:00:00Z");
        var unique = tx("T3", "acct-1", "coffee", "2026-08-16T10:10:00Z");
        var groups = TransactionDeduplicator.exactGroups(List.of(a, b, unique));
        assertThat(groups).hasSize(1);
        assertThat(groups.get(0)).extracting(TransactionDeduplicator.Transaction::transactionId)
                .containsExactly("T1", "T2");
    }
    @Test void findsNearDuplicatesInUnorderedInput() {
        var a = tx("T1", "acct-1", "coffee", "2026-08-16T10:00:00Z");
        var b = tx("T2", "acct-1", "coffee", "2026-08-16T10:03:00Z");
        var c = tx("T3", "acct-1", "coffee", "2026-08-16T11:00:00Z");
        assertThat(TransactionDeduplicator.withinWindow(List.of(c, b, a), Duration.ofMinutes(5))).hasSize(1);
    }
    private TransactionDeduplicator.Transaction tx(String id, String account, String merchant, String time) {
        return new TransactionDeduplicator.Transaction(id, account, merchant, 1299, "usd", Instant.parse(time));
    }
}
