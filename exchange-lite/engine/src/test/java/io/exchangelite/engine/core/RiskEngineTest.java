package io.exchangelite.engine.core;

import io.exchangelite.common.domain.OrderRequest;
import io.exchangelite.common.domain.OrderSide;
import io.exchangelite.common.domain.OrderType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RiskEngineTest {
    @Test
    void rejectsQuantityAboveLimit() {
        RiskEngine risk = new RiskEngine(100, 10_000);

        RiskDecision decision = risk.evaluate(new OrderRequest("BTC-USD", "B-1", "acct", OrderSide.BUY, OrderType.LIMIT, 10, 101));

        assertFalse(decision.accepted());
    }

    @Test
    void rejectsNotionalAboveLimit() {
        RiskEngine risk = new RiskEngine(100, 10_000);

        RiskDecision decision = risk.evaluate(new OrderRequest("BTC-USD", "B-1", "acct", OrderSide.BUY, OrderType.LIMIT, 101, 100));

        assertFalse(decision.accepted());
    }

    @Test
    void acceptsOrderWithinLimits() {
        RiskEngine risk = new RiskEngine(100, 10_000);

        RiskDecision decision = risk.evaluate(new OrderRequest("BTC-USD", "B-1", "acct", OrderSide.BUY, OrderType.LIMIT, 100, 100));

        assertTrue(decision.accepted());
    }
}
