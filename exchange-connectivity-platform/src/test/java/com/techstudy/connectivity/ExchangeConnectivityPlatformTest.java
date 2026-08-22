package com.techstudy.connectivity;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static com.techstudy.connectivity.ExchangeConnectivityPlatform.*;
import static org.junit.jupiter.api.Assertions.*;

class ExchangeConnectivityPlatformTest {
    @Test
    void suppressesDuplicateAndPersistsSequenceAcrossFailover() {
        List<JournalEntry> journal = new ArrayList<>();
        FencingLease lease = new FencingLease();
        VenueSession first = session("a", lease, journal, new TokenBucket(10, 10));
        first.promoteAndLogon();
        SendResult accepted = first.send(order("id-1"), TransmissionOutcome.ACK);
        assertEquals(1, accepted.outboundSequence());
        assertTrue(first.send(order("id-1"), TransmissionOutcome.ACK).detail().contains("duplicate"));

        VenueSession standby = session("b", lease, journal, new TokenBucket(10, 10));
        standby.promoteAndLogon();
        assertEquals(2, standby.nextOutboundSequence());
        assertEquals(OrderState.ACKNOWLEDGED, standby.orderSnapshot().get("id-1"));
        assertEquals(OrderState.REJECTED, first.send(order("stale"), TransmissionOutcome.ACK).state());
    }

    @Test
    void marksDisconnectAfterWriteAsUnknownUntilReconciled() {
        List<JournalEntry> journal = new ArrayList<>();
        FencingLease lease = new FencingLease();
        VenueSession session = session("a", lease, journal, new TokenBucket(10, 10));
        session.promoteAndLogon();
        assertEquals(OrderState.UNKNOWN, session.send(order("id-2"), TransmissionOutcome.DISCONNECT_AFTER_WRITE).state());

        VenueSession standby = session("b", lease, journal, new TokenBucket(10, 10));
        standby.promoteAndLogon();
        assertEquals(2, standby.send(order("later-order"), TransmissionOutcome.ACK).outboundSequence());
        standby.reconcileUnknown("id-2", OrderState.ACKNOWLEDGED);

        assertEquals(OrderState.ACKNOWLEDGED, standby.orderSnapshot().get("id-2"));
        JournalEntry reconciliation = journal.get(journal.size() - 1);
        assertEquals(1, reconciliation.sequence(), "reconciliation must retain the uncertain order's sequence");
    }

    @Test
    void detectsSequenceGapAndIgnoresDuplicateInboundMessages() {
        List<JournalEntry> journal = new ArrayList<>();
        FencingLease lease = new FencingLease();
        VenueSession session = session("a", lease, journal, new TokenBucket(10, 10));
        session.promoteAndLogon();
        assertEquals("RESEND_REQUEST:1-2", session.onInbound(3, "id", OrderState.ACKNOWLEDGED));
        assertEquals("APPLIED:1", session.onInbound(1, "id", OrderState.ACKNOWLEDGED));
        assertEquals("DUPLICATE_IGNORED:1", session.onInbound(1, "id", OrderState.ACKNOWLEDGED));

        VenueSession recovered = session("b", lease, journal, new TokenBucket(10, 10));
        recovered.promoteAndLogon();
        assertEquals("DUPLICATE_IGNORED:1", recovered.onInbound(1, "id", OrderState.ACKNOWLEDGED));
        assertEquals("APPLIED:2", recovered.onInbound(2, "id", OrderState.ACKNOWLEDGED));
    }

    @Test
    void enforcesVenueThrottle() {
        AtomicLong nanos = new AtomicLong();
        VenueSession session = session("a", new FencingLease(), new ArrayList<>(), new TokenBucket(1, 1, nanos::get));
        session.promoteAndLogon();
        assertEquals(OrderState.ACKNOWLEDGED, session.send(order("first"), TransmissionOutcome.ACK).state());
        assertTrue(session.send(order("second"), TransmissionOutcome.ACK).detail().contains("throttle"));
        nanos.addAndGet(1_000_000_000L);
        assertEquals(OrderState.ACKNOWLEDGED, session.send(order("third"), TransmissionOutcome.ACK).state());
    }

    private VenueSession session(String id, FencingLease lease, List<JournalEntry> journal, TokenBucket bucket) {
        return new VenueSession(id, "XNAS", Protocol.FIX, bucket, lease, journal);
    }

    private OrderIntent order(String id) {
        return new OrderIntent(id, "AAPL", Side.BUY, 10, 20_000);
    }
}
