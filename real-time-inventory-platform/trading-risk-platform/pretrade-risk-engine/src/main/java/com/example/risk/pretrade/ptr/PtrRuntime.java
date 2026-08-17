package com.example.risk.pretrade.ptr;

import com.example.risk.pretrade.ptr.ControlPlane.*;
import com.example.risk.pretrade.ptr.PtrCore.*;
import io.micrometer.core.instrument.*;
import org.springframework.stereotype.Service;
import java.time.Clock;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

@Service
public final class PtrRuntime implements AutoCloseable {
    private final LocalDsfBus bus=new LocalDsfBus(); private final RiskState state=new RiskState(4096);private final ExposurePool pool=new ExposurePool(8192);
    private final ListenableCache<String,RiskConfig> cache=new ListenableCache<>();private final CommittedLimitStore limits=new CommittedLimitStore();private final ConfigWriter writer=new ConfigWriter("control-plane-1",cache);
    private final ConcurrentLinkedDeque<Decision> recent=new ConcurrentLinkedDeque<>();private final Counter pass,block,breaches;private final io.micrometer.core.instrument.Timer latency;private final AtomicLong sequence=new AtomicLong();private final Recovery.InMemoryJournal journal=new Recovery.InMemoryJournal();
    public PtrRuntime(MeterRegistry registry){cache.listen(limits);pass=Counter.builder("ptr.decisions").tag("outcome","PASS").register(registry);block=Counter.builder("ptr.decisions").tag("outcome","BLOCK").register(registry);breaches=Counter.builder("ptr.breaches").register(registry);latency=io.micrometer.core.instrument.Timer.builder("ptr.order.latency").publishPercentiles(.5,.99,.999).register(registry);Gauge.builder("ptr.queue.depth",bus,LocalDsfBus::depth).register(registry);Gauge.builder("ptr.pool.borrowed",pool,ExposurePool::borrowed).register(registry);Gauge.builder("ptr.pool.available",pool,ExposurePool::available).register(registry);
        OrderRateWindow rate=new OrderRateWindow(4096);RiskCheckGroup checks=PtrCore.standardChecks(rate);ExposureGroup leaf=new ExposureGroup(1,new int[0],checks);ExposureGroup root=new ExposureGroup(0,new int[]{1,1},new RiskCheckGroup(List.of()));ExposureDag dag=new ExposureDag(new ExposureGroup[]{root,leaf});OrderHandler handler=new OrderHandler(state,pool,dag,limits,this::record,Clock.systemUTC());new MessageRegistry(bus).register(Order.class,handler);
    }
    /** Non-REST ingress used by the demo/load generator; models a DSF publication. */
    public void submit(Order order){journal.append(new Recovery.OrderEvent(sequence.incrementAndGet(),order));bus.publish(order);}
    private void record(Decision d){latency.record(d.latencyNanos(),TimeUnit.NANOSECONDS);if(d.outcome()==Outcome.PASS)pass.increment();else{block.increment();breaches.increment();}recent.addFirst(d);while(recent.size()>100)recent.pollLast();}
    public RiskConfig write(RiskConfig c){return writer.apply(c);}public RuntimeView view(){return new RuntimeView(bus.depth(),pool.borrowed(),pool.available(),limits.current(),List.copyOf(recent),sequence.get(),pass.count(),block.count());}
    public record RuntimeView(int queueDepth,int poolBorrowed,int poolAvailable,Limits limits,List<Decision> recentDecisions,long journalSequence,double passes,double blocks){}
    public void close(){bus.close();}
}

