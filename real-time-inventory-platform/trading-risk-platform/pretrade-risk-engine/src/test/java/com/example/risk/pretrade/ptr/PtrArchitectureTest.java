package com.example.risk.pretrade.ptr;

import com.example.risk.pretrade.ptr.ControlPlane.*;
import com.example.risk.pretrade.ptr.PtrCore.*;
import com.example.risk.pretrade.ptr.Recovery.*;
import org.junit.jupiter.api.Test;
import java.time.*;
import java.util.*;
import static org.assertj.core.api.Assertions.*;

class PtrArchitectureTest {
    private static Order order(long id,long qty){return new Order(id,1,7,Side.BUY,qty,10_0000,10_0000,System.nanoTime());}
    private static OrderHandler handler(RiskState state,ExposurePool pool,LimitsProvider limits,List<Decision> out){
        var rate=new OrderRateWindow(16);var leaf=new ExposureGroup(1,new int[0],PtrCore.standardChecks(rate));
        // root references leaf twice: visit token must prevent duplicate OrderRate evaluation.
        var dag=new ExposureDag(new ExposureGroup[]{new ExposureGroup(0,new int[]{1,1},new RiskCheckGroup(List.of())),leaf});
        return new OrderHandler(state,pool,dag,limits,out::add,Clock.systemUTC());
    }
    @Test void rejectedOrderRollsBackEveryMutation(){var state=new RiskState(16);var pool=new ExposurePool(2);var out=new ArrayList<Decision>();var h=handler(state,pool,()->new Limits(10,10,Long.MAX_VALUE,100,99),out);h.onMessage(order(1,11));assertThat(out.getFirst().outcome()).isEqualTo(Outcome.BLOCK);assertThat(state.openBuy(1)).isZero();assertThat(pool.borrowed()).isZero();}
    @Test void poolReferenceCountControlsRecycling(){var pool=new ExposurePool(1);var e=pool.borrow(order(1,1));e.retain();e.release();assertThat(e.references()).isOne();assertThat(pool.borrowed()).isOne();e.release();assertThat(pool.borrowed()).isZero();assertThat(pool.available()).isOne();}
    @Test void duplicateCommittedEventIsIdempotentAndLostVersionIsRejected(){var cache=new ListenableCache<String,RiskConfig>();var store=new CommittedLimitStore();cache.listen(store);var writer=new ConfigWriter("writer",cache);var limits=new Limits(1,2,3,4,5);var committed=new RiskConfig("global",2,Lifecycle.COMMITTED,limits,"writer","event-2");writer.apply(committed);assertThat(writer.apply(committed)).isSameAs(committed);assertThatThrownBy(()->writer.apply(new RiskConfig("global",1,Lifecycle.COMMITTED,limits,"writer","late-event"))).isInstanceOf(IllegalArgumentException.class);assertThat(store.current()).isEqualTo(limits);}
    @Test void partialLifecycleUpdateDoesNotReachHotStore(){var cache=new ListenableCache<String,RiskConfig>();var store=new CommittedLimitStore();cache.listen(store);var original=store.current();cache.put("x",new RiskConfig("x",1,Lifecycle.STAGED,new Limits(1,1,1,1,1),"writer","x1"));assertThat(store.current()).isEqualTo(original);}
    @Test void replayUsesLiveHandlerAndRestoresOnlyTail(){var state=new RiskState(16);var pool=new ExposurePool(4);var out=new ArrayList<Decision>();var h=handler(state,pool,()->new Limits(100,100,Long.MAX_VALUE,100,99),out);h.onMessage(order(1,5));var snap=new EngineSnapshot(1,state.snapshot());var journal=new InMemoryJournal();journal.append(new OrderEvent(1,order(1,5)));journal.append(new OrderEvent(2,order(2,7)));var recovered=new RiskState(16);var recoveredOut=new ArrayList<Decision>();Recovery.recover(snap,recovered,journal,handler(recovered,pool,()->new Limits(100,100,Long.MAX_VALUE,100,99),recoveredOut));assertThat(recovered.openBuy(1)).isEqualTo(12);assertThat(recoveredOut).hasSize(1);}
    @Test void delegationRequiresDelegateAce(){var acl=new AclService();acl.grant("admin",new Ace("desk:A","lead",Set.of(Permission.DELEGATE),null));acl.grant("lead",new Ace("desk:A","trader",Set.of(Permission.VIEW),"lead"));assertThat(acl.allowed("desk:A","trader",Permission.VIEW)).isTrue();assertThatThrownBy(()->acl.grant("intruder",new Ace("desk:A","other",Set.of(Permission.EDIT),"intruder"))).isInstanceOf(SecurityException.class);}
    @Test void standbyTakesOverAndFormerPrimaryFailsClosed(){var clock=new MutableClock(Instant.parse("2026-01-01T00:00:00Z"));var lease=new InMemoryLeaseStore(clock);var a=new LeaderElector("a",lease,clock,Duration.ofSeconds(2));var b=new LeaderElector("b",lease,clock,Duration.ofSeconds(2));assertThat(a.campaign()).isTrue();assertThat(b.campaign()).isFalse();clock.advance(Duration.ofSeconds(3));assertThat(b.campaign()).isTrue();assertThatThrownBy(a::assertPrimary).hasMessageContaining("split-brain");}
    static final class MutableClock extends Clock {private Instant now;MutableClock(Instant now){this.now=now;}void advance(Duration d){now=now.plus(d);}public ZoneId getZone(){return ZoneOffset.UTC;}public Clock withZone(ZoneId z){return this;}public Instant instant(){return now;}}
}
