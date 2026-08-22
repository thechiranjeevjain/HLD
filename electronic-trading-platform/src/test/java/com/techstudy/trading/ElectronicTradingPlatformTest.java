package com.techstudy.trading;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import static com.techstudy.trading.ElectronicTradingPlatform.*;
import static org.junit.jupiter.api.Assertions.*;

class ElectronicTradingPlatformTest {
    private final Instant now = Instant.parse("2026-08-22T10:00:00Z");
    private final Clock clock = Clock.fixed(now, ZoneOffset.UTC);
    private List<TradingEvent> journal;
    private ElectronicTradingPlatform platform;

    @BeforeEach void setUp() {
        journal = new ArrayList<>();
        platform = new ElectronicTradingPlatform(100, 1_000_000, clock, journal);
        platform.marketData().onQuote("AAPL", 1_000, now);
    }

    @Test void runsGatewayRiskOmsConnectivityExecutionAndPositionFlow() {
        OrderView result = platform.submit("demo-key", order("o1", 10), VenueOutcome.ACK_AND_FILL);
        assertEquals(OrderState.FILLED, result.state());
        assertEquals(10, platform.positions().position("acct", "AAPL"));
        assertEquals(1, platform.connectivity().messagesSent());
    }

    @Test void rejectsBeforeConnectivityWhenRiskFails() {
        assertEquals(OrderState.RISK_REJECTED, platform.submit("demo-key", order("large", 101), VenueOutcome.ACK_AND_FILL).state());
        assertEquals(0, platform.connectivity().messagesSent());
    }

    @Test void isIdempotentAndReconcilesUncertainVenueOutcome() {
        OrderRequest request = order("uncertain", 20);
        assertEquals(OrderState.UNKNOWN, platform.submit("demo-key", request, VenueOutcome.DISCONNECT_AFTER_WRITE).state());
        assertEquals(20, platform.risk().pending("acct"));
        assertEquals(OrderState.UNKNOWN, platform.submit("demo-key", request, VenueOutcome.ACK_AND_FILL).state());
        assertEquals(1, platform.connectivity().messagesSent());
        assertEquals(OrderState.FILLED, platform.reconcileUnknown("uncertain", true).state());
        assertEquals(20, platform.positions().position("acct", "AAPL"));
    }

    @Test void recoversOmsAndPositionsFromJournal() {
        platform.submit("demo-key", order("o1", 10), VenueOutcome.ACK_AND_FILL);
        ElectronicTradingPlatform recovered = new ElectronicTradingPlatform(100, 1_000_000, clock, journal);
        recovered.recoverFromJournal();
        assertEquals(OrderState.FILLED, recovered.oms().find("o1").orElseThrow().state());
        assertEquals(10, recovered.positions().position("acct", "AAPL"));
    }

    @Test void netsOppositePendingOrdersForRiskWithoutLosingReservations() {
        assertEquals(OrderState.UNKNOWN, platform.submit("demo-key", order("buy", 80), VenueOutcome.DISCONNECT_AFTER_WRITE).state());
        OrderRequest sell = new OrderRequest("sell", "acct", "AAPL", Side.SELL, 80);
        assertEquals(OrderState.FILLED, platform.submit("demo-key", sell, VenueOutcome.ACK_AND_FILL).state());
        assertEquals(-80, platform.positions().position("acct", "AAPL"));
        assertEquals(80, platform.risk().pending("acct"));
    }

    private OrderRequest order(String id, long quantity) { return new OrderRequest(id, "acct", "AAPL", Side.BUY, quantity); }
}
