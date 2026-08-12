package com.example.risk.pretrade.ptr;

import com.example.risk.pretrade.ptr.PtrCore.*;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public final class Recovery {
    private Recovery() {}
    public record OrderEvent(long sequence,Order order){}
    public record EngineSnapshot(long lastSequence,RiskState.Snapshot state){}
    public interface EventJournal { void append(OrderEvent e); List<OrderEvent> after(long sequence); }
    public static final class InMemoryJournal implements EventJournal {
        private final List<OrderEvent> events=new CopyOnWriteArrayList<>();public void append(OrderEvent e){events.add(e);}public List<OrderEvent> after(long s){return events.stream().filter(e->e.sequence>s).toList();}
    }
    /** Replay invokes the exact live InputHandler, avoiding a second state-transition implementation. */
    public static void recover(EngineSnapshot snapshot,RiskState state,EventJournal journal,InputHandler<Order> liveHandler){state.restore(snapshot.state);journal.after(snapshot.lastSequence).forEach(e->liveHandler.onMessage(e.order));}
}

