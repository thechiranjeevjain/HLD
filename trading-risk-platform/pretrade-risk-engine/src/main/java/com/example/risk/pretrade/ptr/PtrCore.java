package com.example.risk.pretrade.ptr;

import java.time.Clock;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.function.Consumer;

/** Hot-path model. Money and prices are fixed-point longs (four decimal places). */
public final class PtrCore {
    private PtrCore() {}

    public enum Side { BUY, SELL }
    public enum Outcome { PASS, BLOCK }
    public record Order(long id, int accountId, int instrumentId, Side side, long quantity,
                        long priceE4, long marketPriceE4, long receivedNanos) {}
    public record Decision(long orderId, Outcome outcome, String reason, long latencyNanos) {}
    public record CompositeIdentity(int accountId, int instrumentId) {}

    /** Pooled, explicitly shared object. retain/release catches lifecycle bugs immediately. */
    public static final class Exposure implements AutoCloseable {
        private final AtomicInteger references = new AtomicInteger();
        private ExposurePool owner;
        public long orderId, quantity, notionalE4;
        public int accountId, instrumentId;
        public Side side;
        private void initialize(ExposurePool pool, Order order) {
            owner = pool; orderId = order.id(); accountId = order.accountId(); instrumentId = order.instrumentId();
            side = order.side(); quantity = order.quantity(); notionalE4 = Math.multiplyExact(order.quantity(), order.priceE4());
            references.set(1);
        }
        public Exposure retain() { if (references.incrementAndGet() <= 1) throw new IllegalStateException("retain after release"); return this; }
        public void release() { int value = references.decrementAndGet(); if (value < 0) throw new IllegalStateException("double release"); if (value == 0) owner.recycle(this); }
        public int references() { return references.get(); }
        @Override public void close() { release(); }
        private void clear() { owner = null; side = null; orderId = quantity = notionalE4 = 0; accountId = instrumentId = 0; }
    }

    public static final class ExposurePool {
        private final ArrayBlockingQueue<Exposure> available;
        private final AtomicInteger borrowed = new AtomicInteger();
        public ExposurePool(int capacity) { available = new ArrayBlockingQueue<>(capacity); for (int i=0;i<capacity;i++) available.add(new Exposure()); }
        public Exposure borrow(Order order) { Exposure e=available.poll(); if(e==null) throw new IllegalStateException("exposure pool exhausted"); borrowed.incrementAndGet(); e.initialize(this, order); return e; }
        private void recycle(Exposure e) { e.clear(); borrowed.decrementAndGet(); if(!available.offer(e)) throw new IllegalStateException("pool overflow"); }
        public int borrowed() { return borrowed.get(); }
        public int available() { return available.size(); }
    }

    /** Primitive arrays keep the mutable working set compact and owned by one event-loop thread. */
    public static final class RiskState {
        private final long[] openBuy, openSell;
        private long ownerThread = -1;
        public RiskState(int identities) { openBuy=new long[identities]; openSell=new long[identities]; }
        public void claimOwner() { long id=Thread.currentThread().threadId(); if(ownerThread==-1) ownerThread=id; if(ownerThread!=id) throw new IllegalStateException("mutable risk state accessed outside owner thread"); }
        public long openBuy(int id) { return openBuy[id]; }
        public long openSell(int id) { return openSell[id]; }
        public Transaction begin(int id, Side side, long quantity) { claimOwner(); return new Transaction(this,id,side,quantity); }
        public Snapshot snapshot() { return new Snapshot(openBuy.clone(),openSell.clone()); }
        public void restore(Snapshot s) { System.arraycopy(s.buy(),0,openBuy,0,openBuy.length); System.arraycopy(s.sell(),0,openSell,0,openSell.length); }
        public record Snapshot(long[] buy,long[] sell) {}
    }
    public static final class Transaction implements AutoCloseable {
        private final RiskState state; private final int id; private final Side side; private final long quantity; private boolean finished;
        private Transaction(RiskState state,int id,Side side,long quantity){this.state=state;this.id=id;this.side=side;this.quantity=quantity; if(side==Side.BUY) state.openBuy[id]+=quantity; else state.openSell[id]+=quantity;}
        public void commit(){ finished=true; }
        public void rollback(){ if(finished)return; if(side==Side.BUY)state.openBuy[id]-=quantity;else state.openSell[id]-=quantity;finished=true; }
        @Override public void close(){ rollback(); }
    }

    public record Limits(long maxOpenBuy,long maxOpenSell,long maxOrderValueE4,int maxDeviationBps,int maxOrdersPerSecond) {}
    public interface Calculator { long calculate(Context context); }
    public interface Validator { Optional<String> validate(long value, Context context); }
    public record Context(Order order, Exposure exposure, RiskState state, int identityIndex, Limits limits, long nowMillis) {}
    public record RiskCheck(String name, Calculator calculator, Validator validator) {
        Optional<String> evaluate(Context c){return validator.validate(calculator.calculate(c),c).map(v->name+": "+v);}
    }
    public record RiskCheckGroup(List<RiskCheck> checks) {}
    public static final class ExposureGroup {
        public final int id; public final int[] children; public final RiskCheckGroup checks;
        public ExposureGroup(int id,int[] children,RiskCheckGroup checks){this.id=id;this.children=children;this.checks=checks;}
    }
    public static final class ExposureDag {
        private final ExposureGroup[] groups; private final int[] visited; private int token;
        public ExposureDag(ExposureGroup[] groups){this.groups=groups;visited=new int[groups.length];}
        public Optional<String> evaluate(int[] roots,Context c){int t=++token;if(t==0){Arrays.fill(visited,0);t=++token;} for(int root:roots){var failure=visit(root,t,c);if(failure.isPresent())return failure;}return Optional.empty();}
        private Optional<String> visit(int id,int t,Context c){if(visited[id]==t)return Optional.empty();visited[id]=t;ExposureGroup g=groups[id];for(RiskCheck check:g.checks.checks()){var f=check.evaluate(c);if(f.isPresent())return f;}for(int child:g.children){var f=visit(child,t,c);if(f.isPresent())return f;}return Optional.empty();}
    }

    public static RiskCheckGroup standardChecks(OrderRateWindow rate) {
        return new RiskCheckGroup(List.of(
                new RiskCheck("OpenBuy", c->c.state.openBuy(c.identityIndex), (v,c)->v>c.limits.maxOpenBuy?Optional.of(v+" > "+c.limits.maxOpenBuy):Optional.empty()),
                new RiskCheck("OpenSell", c->c.state.openSell(c.identityIndex), (v,c)->v>c.limits.maxOpenSell?Optional.of(v+" > "+c.limits.maxOpenSell):Optional.empty()),
                new RiskCheck("MaxOrderValue", c->c.exposure.notionalE4, (v,c)->v>c.limits.maxOrderValueE4?Optional.of(v+" > "+c.limits.maxOrderValueE4):Optional.empty()),
                new RiskCheck("PriceDeviation", c->Math.abs(c.order.priceE4-c.order.marketPriceE4)*10_000/c.order.marketPriceE4, (v,c)->v>c.limits.maxDeviationBps?Optional.of(v+"bps > "+c.limits.maxDeviationBps):Optional.empty()),
                new RiskCheck("OrderRate", c->rate.increment(c.identityIndex,c.nowMillis), (v,c)->v>c.limits.maxOrdersPerSecond?Optional.of(v+"/s > "+c.limits.maxOrdersPerSecond):Optional.empty())
        ));
    }
    public static final class OrderRateWindow {
        private final long[] seconds; private final int[] counts;
        public OrderRateWindow(int identities){seconds=new long[identities];counts=new int[identities];}
        long increment(int id,long ms){long second=ms/1000;if(seconds[id]!=second){seconds[id]=second;counts[id]=0;}return ++counts[id];}
    }

    public interface MessageBus { <T> void subscribe(Class<T> type, Consumer<T> handler); void publish(Object message); int depth(); }
    public static final class LocalDsfBus implements MessageBus, AutoCloseable {
        private final BlockingQueue<Object> queue=new ArrayBlockingQueue<>(65_536); private final Map<Class<?>,Consumer<Object>> handlers=new ConcurrentHashMap<>(); private final Thread owner; private volatile boolean running=true;
        public LocalDsfBus(){owner=Thread.ofPlatform().name("risk-state-owner").start(()->{while(running||!queue.isEmpty()){try{Object m=queue.poll(100,TimeUnit.MILLISECONDS);if(m!=null){Consumer<Object> h=handlers.get(m.getClass());if(h!=null)h.accept(m);}}catch(InterruptedException e){Thread.currentThread().interrupt();return;}}});}
        @SuppressWarnings("unchecked") public <T> void subscribe(Class<T> t,Consumer<T> h){handlers.put(t,(Consumer<Object>)h);}
        public void publish(Object m){if(!queue.offer(m))throw new RejectedExecutionException("risk queue full");}
        public int depth(){return queue.size();}
        public void close(){running=false;owner.interrupt();}
    }
    public static final class MessageRegistry {
        private final MessageBus bus; public MessageRegistry(MessageBus bus){this.bus=bus;}
        public <T> void register(Class<T> type, InputHandler<T> handler){bus.subscribe(type,handler::onMessage);}
    }
    public interface InputHandler<T>{void onMessage(T message);}

    public static final class OrderHandler implements InputHandler<Order> {
        private final RiskState state; private final ExposurePool pool; private final ExposureDag dag; private final LimitsProvider limits; private final Consumer<Decision> sink; private final Clock clock;
        public OrderHandler(RiskState state,ExposurePool pool,ExposureDag dag,LimitsProvider limits,Consumer<Decision> sink,Clock clock){this.state=state;this.pool=pool;this.dag=dag;this.limits=limits;this.sink=sink;this.clock=clock;}
        public void onMessage(Order order){long start=System.nanoTime();Exposure e=pool.borrow(order);try(e;Transaction tx=state.begin(order.accountId(),order.side(),order.quantity())){Context c=new Context(order,e,state,order.accountId(),limits.current(),clock.millis());Optional<String> failure=dag.evaluate(new int[]{0},c);if(failure.isPresent()){tx.rollback();sink.accept(new Decision(order.id(),Outcome.BLOCK,failure.get(),System.nanoTime()-start));}else{tx.commit();sink.accept(new Decision(order.id(),Outcome.PASS,"all checks passed",System.nanoTime()-start));}}catch(RuntimeException ex){sink.accept(new Decision(order.id(),Outcome.BLOCK,"fail closed: "+ex.getMessage(),System.nanoTime()-start));}}
    }
    public interface LimitsProvider { Limits current(); }
}

