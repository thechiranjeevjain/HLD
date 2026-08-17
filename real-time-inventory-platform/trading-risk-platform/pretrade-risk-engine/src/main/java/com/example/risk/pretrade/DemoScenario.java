package com.example.risk.pretrade;

import com.example.risk.pretrade.ptr.ControlPlane.*;
import com.example.risk.pretrade.ptr.PtrCore.*;
import com.example.risk.pretrade.ptr.PtrRuntime;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import java.time.*;
import java.util.concurrent.*;

@Component @Profile("demo")
public final class DemoScenario implements CommandLineRunner {
    private final PtrRuntime runtime; public DemoScenario(PtrRuntime runtime){this.runtime=runtime;}
    public void run(String... args)throws Exception{
        System.out.println("1 CONFIG: NEW -> ADDED -> STAGED -> COMMITTED");
        Limits initial=new Limits(20_000,20_000,5_000_000_000L,500,50_000);RiskConfig c=new RiskConfig("global",1,Lifecycle.NEW,initial,"control-plane-1","cfg-1-new");for(Lifecycle next:new Lifecycle[]{Lifecycle.ADDED,Lifecycle.STAGED,Lifecycle.COMMITTED}){c=c.transition(next);c=new RiskConfig(c.id(),c.version(),c.lifecycle(),c.limits(),c.writer(),"cfg-1-"+next);runtime.write(c);}
        System.out.println("2 LOAD: publishing 2,000 orders concurrently onto DSF abstraction (evaluation remains single-owner)");
        try(var executor=Executors.newVirtualThreadPerTaskExecutor()){for(int i=0;i<2_000;i++){long id=i;executor.submit(()->runtime.submit(new Order(id,1,7,Side.BUY,10,10_0000,10_0000,System.nanoTime())));}}
        awaitDrain();printSummary("3 DECISIONS/METRICS");
        System.out.println("4 LIMIT CHANGE: COMMITTED maxOpenBuy=20000 -> 20001, then trigger breach");
        runtime.write(new RiskConfig("global",2,Lifecycle.COMMITTED,new Limits(20_001,20_000,5_000_000_000L,500,50_000),"control-plane-1","cfg-2"));
        runtime.submit(new Order(99_999,1,7,Side.BUY,2,10_0000,10_0000,System.nanoTime()));awaitDrain();printSummary("5 PASS/BLOCK + SIDECAR VIEW");
        var lease=new InMemoryLeaseStore(Clock.systemUTC());var primary=new LeaderElector("primary",lease,Clock.systemUTC(),Duration.ofSeconds(5));var standby=new LeaderElector("standby",lease,Clock.systemUTC(),Duration.ofSeconds(5));primary.campaign();primary.stop();System.out.println("6 FAILOVER standby-takeover="+standby.campaign());
        System.out.println("7 RECOVERY is exercised by PtrArchitectureTest: snapshot + journal tail through OrderHandler");
    }
    private void awaitDrain()throws InterruptedException{long until=System.nanoTime()+TimeUnit.SECONDS.toNanos(10);while(runtime.view().queueDepth()>0&&System.nanoTime()<until)Thread.sleep(10);Thread.sleep(50);}
    private void printSummary(String step){var view=runtime.view();System.out.println(step+": passes="+view.passes()+", blocks="+view.blocks()+", queue="+view.queueDepth()+", poolBorrowed="+view.poolBorrowed()+", latest="+(view.recentDecisions().isEmpty()?"none":view.recentDecisions().getFirst()));}
}
